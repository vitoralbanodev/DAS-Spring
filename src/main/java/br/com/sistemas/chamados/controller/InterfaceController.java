package br.com.sistemas.chamados.controller;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.ComentarioService;
import br.com.sistemas.chamados.service.TecnicoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/app")
public class InterfaceController {

    private final ChamadoService chamadoService;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;
    private final ComentarioService comentarioService;

    public InterfaceController(ChamadoService chamadoService, ClienteService clienteService,
            CategoriaService categoriaService, TecnicoService tecnicoService,
            ComentarioService comentarioService) {
        this.chamadoService = chamadoService;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
        this.comentarioService = comentarioService;
    }

    @GetMapping
    public String painel(Model model) {
        Pageable recentes = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "dataAbertura"));
        model.addAttribute("chamados", chamadoService.listar(null, null, null, recentes));
        model.addAttribute("abertos", contar(StatusChamado.ABERTO));
        model.addAttribute("emAtendimento", contar(StatusChamado.EM_ATENDIMENTO));
        model.addAttribute("aguardando", contar(StatusChamado.AGUARDANDO_CLIENTE));
        model.addAttribute("fechados", contar(StatusChamado.FECHADO));
        adicionarReferencias(model);
        return "app/painel";
    }

    @GetMapping("/chamados")
    public String listarChamados(@RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long clienteId,
            @org.springframework.data.web.PageableDefault(size = 10, sort = "dataAbertura",
                    direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        Page<ChamadoResponse> chamados = chamadoService.listar(status, prioridade, clienteId, pageable);
        model.addAttribute("chamados", chamados);
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("prioridadeSelecionada", prioridade);
        model.addAttribute("clienteSelecionado", clienteId);
        adicionarFiltros(model);
        adicionarReferencias(model);
        return "app/chamados";
    }

    @GetMapping("/chamados/novo")
    public String novoChamado(Model model) {
        model.addAttribute("chamado", new ChamadoRequest(null, null, null, null, null, null, null));
        adicionarFormularioChamado(model);
        return "app/chamado-form";
    }

    @PostMapping("/chamados")
    public String criarChamado(@Valid @ModelAttribute("chamado") ChamadoRequest chamado,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            adicionarFormularioChamado(model);
            return "app/chamado-form";
        }
        try {
            ChamadoResponse criado = chamadoService.criar(chamado);
            redirect.addFlashAttribute("sucesso", "Chamado aberto com sucesso.");
            return "redirect:/app/chamados/" + criado.id();
        } catch (RegraNegocioException erro) {
            model.addAttribute("erro", erro.getMessage());
            adicionarFormularioChamado(model);
            return "app/chamado-form";
        }
    }

    @GetMapping("/chamados/{id}")
    public String detalheChamado(@PathVariable Long id, Model model) {
        ChamadoDetalheResponse chamado = chamadoService.buscar(id);
        model.addAttribute("chamado", chamado);
        model.addAttribute("comentario", new ComentarioRequest(null, null));
        adicionarReferencias(model);
        adicionarFiltros(model);
        return "app/chamado-detalhe";
    }

    @PostMapping("/chamados/{id}/status")
    public String alterarStatus(@PathVariable Long id, @RequestParam StatusChamado status,
            RedirectAttributes redirect) {
        try {
            chamadoService.alterarStatus(id, status);
            redirect.addFlashAttribute("sucesso", "Status atualizado.");
        } catch (RegraNegocioException erro) {
            redirect.addFlashAttribute("erro", erro.getMessage());
        }
        return "redirect:/app/chamados/" + id;
    }

    @PostMapping("/chamados/{id}/tecnico")
    public String atribuirTecnico(@PathVariable Long id, @RequestParam Long tecnicoId,
            RedirectAttributes redirect) {
        try {
            chamadoService.atribuirTecnico(id, tecnicoId);
            redirect.addFlashAttribute("sucesso", "Técnico atribuído ao chamado.");
        } catch (RegraNegocioException erro) {
            redirect.addFlashAttribute("erro", erro.getMessage());
        }
        return "redirect:/app/chamados/" + id;
    }

    @PostMapping("/chamados/{id}/comentarios")
    public String adicionarComentario(@PathVariable Long id,
            @Valid @ModelAttribute("comentario") ComentarioRequest comentario,
            BindingResult resultado, Model model) {
        if (resultado.hasErrors()) {
            model.addAttribute("chamado", chamadoService.buscar(id));
            adicionarReferencias(model);
            adicionarFiltros(model);
            return "app/chamado-detalhe";
        }
        comentarioService.criar(id, comentario);
        return "redirect:/app/chamados/" + id;
    }

    @GetMapping("/cadastros")
    public String cadastros(Model model) {
        prepararCadastros(model);
        return "app/cadastros";
    }

    @PostMapping("/cadastros/clientes")
    public String criarCliente(@Valid @ModelAttribute("novoCliente") ClienteRequest cliente,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            prepararCadastros(model);
            return "app/cadastros";
        }
        try {
            clienteService.criar(cliente);
            redirect.addFlashAttribute("sucesso", "Cliente cadastrado.");
            return "redirect:/app/cadastros";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erroCliente", erro.getMessage());
            prepararCadastros(model);
            return "app/cadastros";
        }
    }

    @PostMapping("/cadastros/categorias")
    public String criarCategoria(@Valid @ModelAttribute("novaCategoria") CategoriaRequest categoria,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            prepararCadastros(model);
            return "app/cadastros";
        }
        try {
            categoriaService.criar(categoria);
            redirect.addFlashAttribute("sucesso", "Categoria cadastrada.");
            return "redirect:/app/cadastros";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erroCategoria", erro.getMessage());
            prepararCadastros(model);
            return "app/cadastros";
        }
    }

    @PostMapping("/cadastros/tecnicos")
    public String criarTecnico(@Valid @ModelAttribute("novoTecnico") TecnicoRequest tecnico,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            prepararCadastros(model);
            return "app/cadastros";
        }
        try {
            tecnicoService.criar(tecnico);
            redirect.addFlashAttribute("sucesso", "Técnico cadastrado.");
            return "redirect:/app/cadastros";
        } catch (RegraNegocioException erro) {
            model.addAttribute("erroTecnico", erro.getMessage());
            prepararCadastros(model);
            return "app/cadastros";
        }
    }

    private long contar(StatusChamado status) {
        return chamadoService.listar(status, null, null, PageRequest.of(0, 1)).getTotalElements();
    }

    private void adicionarFormularioChamado(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("tecnicos", tecnicoService.listar());
        adicionarFiltros(model);
    }

    private void adicionarFiltros(Model model) {
        model.addAttribute("statusOpcoes", StatusChamado.values());
        model.addAttribute("prioridadeOpcoes", Prioridade.values());
        model.addAttribute("clientes", clienteService.listar());
    }

    private void adicionarReferencias(Model model) {
        model.addAttribute("clientesPorId", clienteService.listar().stream()
                .collect(Collectors.toMap(ClienteResponse::id, Function.identity())));
        model.addAttribute("categoriasPorId", categoriaService.listar().stream()
                .collect(Collectors.toMap(CategoriaResponse::id, Function.identity())));
        model.addAttribute("tecnicosPorId", tecnicoService.listar().stream()
                .collect(Collectors.toMap(TecnicoResponse::id, Function.identity())));
    }

    private void prepararCadastros(Model model) {
        if (!model.asMap().containsKey("novoCliente")) {
            model.addAttribute("novoCliente", new ClienteRequest(null, null, null));
        }
        if (!model.asMap().containsKey("novaCategoria")) {
            model.addAttribute("novaCategoria", new CategoriaRequest(null, null));
        }
        if (!model.asMap().containsKey("novoTecnico")) {
            model.addAttribute("novoTecnico", new TecnicoRequest(null, null, null));
        }
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("tecnicos", tecnicoService.listar());
        model.addAttribute("perfis", Arrays.asList(Perfil.values()));
    }
}
