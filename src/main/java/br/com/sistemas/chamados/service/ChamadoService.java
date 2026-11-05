package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.ChamadoDetalheResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ComentarioRepository comentarioRepository;

    public ChamadoService(ChamadoRepository repository, ClienteRepository clienteRepository,
            CategoriaRepository categoriaRepository, TecnicoRepository tecnicoRepository,
            ComentarioRepository comentarioRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Chamado chamado = new Chamado(
            dto.titulo(),
            dto.descricao(),
            dto.prioridade(),
            buscarCliente(dto.clienteId()),
            buscarCategoria(dto.categoriaId()),
            dto.tecnicoId() == null ? null : buscarTecnico(dto.tecnicoId())
        );
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(StatusChamado status, Prioridade prioridade, Long clienteId,
            Pageable pageable) {
        return repository.buscarComFiltros(status, prioridade, clienteId, pageable)
            .map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        buscarCliente(clienteId);
        return repository.findAllByCliente_Id(clienteId, pageable)
            .map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        Chamado chamado = buscarEntity(id);
        List<ComentarioResponse> comentarios =
            comentarioRepository.findByChamado_IdOrderByDataAsc(id).stream()
                .map(ComentarioResponse::de)
                .toList();
        return ChamadoDetalheResponse.de(chamado, comentarios);
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        validarNaoFechado(chamado);
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(buscarCategoria(dto.categoriaId()));
        chamado.setTecnico(dto.tecnicoId() == null ? null : buscarTecnico(dto.tecnicoId()));

        if (dto.status() != null) {
            atualizarStatus(chamado, dto.status());
        }
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse atribuirTecnico(Long id, Long tecnicoId) {
        Chamado chamado = buscarEntity(id);
        validarNaoFechado(chamado);
        chamado.setTecnico(buscarTecnico(tecnicoId));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, StatusChamado status) {
        Chamado chamado = buscarEntity(id);
        validarNaoFechado(chamado);
        atualizarStatus(chamado, status);
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(buscarEntity(id));
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado " + id + " não encontrado"));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente " + id + " não encontrado"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria " + id + " não encontrada"));
    }

    private Tecnico buscarTecnico(Long id) {
        return tecnicoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico " + id + " não encontrado"));
    }

    private void validarNaoFechado(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Não é possível alterar um chamado fechado");
        }
    }

    private void atualizarStatus(Chamado chamado, StatusChamado status) {
        if (status == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
        chamado.setStatus(status);
    }
}
