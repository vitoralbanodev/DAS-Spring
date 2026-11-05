package br.com.sistemas.chamados.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(controllers = ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private ChamadoService chamadoService;

    @Test
    void deveCriarCliente() throws Exception {
        when(clienteService.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Silva", "ana@teste.com", "47999990000"));

        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana Silva","email":"ana@teste.com","telefone":"47999990000"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.nome").value("Ana Silva"));

        verify(clienteService).criar(any(ClienteRequest.class));
    }

    @Test
    void deveValidarDadosObrigatoriosDoCliente() throws Exception {
        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"","email":"email-invalido"}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarConflitoParaEmailDuplicado() throws Exception {
        doThrow(new RegraNegocioException("Já existe um cliente com este e-mail ana@teste.com"))
            .when(clienteService).criar(any(ClienteRequest.class));

        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana Silva","email":"ana@teste.com","telefone":"47999990000"}
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void deveRetornarNaoEncontradoParaClienteInexistente() throws Exception {
        when(clienteService.buscar(999L))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 999 não encontrado"));

        mockMvc.perform(get("/clientes/999"))
            .andExpect(status().isNotFound());
    }
}
