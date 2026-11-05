package br.com.sistemas.chamados.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChamadoFiltroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    private Cliente cliente;
    private Cliente outroCliente;
    private Categoria categoria;

    @BeforeEach
    void prepararDados() {
        cliente = clienteRepository.save(new Cliente("Cliente A", "cliente-a@teste.com", null));
        outroCliente = clienteRepository.save(new Cliente("Cliente B", "cliente-b@teste.com", null));
        categoria = categoriaRepository.save(new Categoria("Categoria teste", null));

        criarChamado("Beta aberto", cliente, Prioridade.ALTA, StatusChamado.ABERTO);
        criarChamado("Alpha aberto", cliente, Prioridade.ALTA, StatusChamado.ABERTO);
        criarChamado("Fechado", cliente, Prioridade.ALTA, StatusChamado.FECHADO);
        criarChamado("Baixa prioridade", cliente, Prioridade.BAIXA, StatusChamado.ABERTO);
        criarChamado("Outro cliente", outroCliente, Prioridade.ALTA, StatusChamado.ABERTO);
    }

    @Test
    void deveCombinarFiltrosPaginacaoEOrdenacao() throws Exception {
        mockMvc.perform(get("/chamados")
                .param("status", "ABERTO")
                .param("prioridade", "ALTA")
                .param("clienteId", cliente.getId().toString())
                .param("page", "0")
                .param("size", "1")
                .param("sort", "titulo,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.numberOfElements").value(1))
            .andExpect(jsonPath("$.content[0].titulo").value("Alpha aberto"));
    }

    @Test
    void deveListarChamadosPaginadosDoCliente() throws Exception {
        mockMvc.perform(get("/clientes/{id}/chamados", cliente.getId())
                .param("page", "0")
                .param("size", "2")
                .param("sort", "titulo,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(4))
            .andExpect(jsonPath("$.numberOfElements").value(2))
            .andExpect(jsonPath("$.content[0].titulo").value("Alpha aberto"));
    }

    @Test
    void deveRetornarNaoEncontradoParaClienteInexistente() throws Exception {
        mockMvc.perform(get("/clientes/{id}/chamados", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    private void criarChamado(String titulo, Cliente dono, Prioridade prioridade, StatusChamado status) {
        Chamado chamado = new Chamado(titulo, "Descrição", prioridade, dono, categoria, null);
        chamado.setStatus(status);
        chamadoRepository.save(chamado);
    }
}
