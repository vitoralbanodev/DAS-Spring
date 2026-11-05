package br.com.sistemas.chamados.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComentarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    private Chamado chamado;

    @BeforeEach
    void prepararChamado() {
        Cliente cliente = clienteRepository.save(new Cliente("Cliente teste", "comentario@teste.com", null));
        Categoria categoria = categoriaRepository.save(new Categoria("Categoria teste", null));
        chamado = chamadoRepository.save(
            new Chamado("Chamado teste", "Descrição", Prioridade.MEDIA, cliente, categoria, null)
        );
    }

    @Test
    void deveCriarComentarioEListarNoDetalheDoChamado() throws Exception {
        mockMvc.perform(post("/chamados/{id}/comentarios", chamado.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"texto":"Estou analisando o chamado.","autor":"Técnico"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.texto").value("Estou analisando o chamado."))
            .andExpect(jsonPath("$.autor").value("Técnico"))
            .andExpect(jsonPath("$.data").isNotEmpty());

        mockMvc.perform(get("/chamados/{id}", chamado.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comentarios.length()").value(1))
            .andExpect(jsonPath("$.comentarios[0].texto").value("Estou analisando o chamado."));
    }

    @Test
    void deveValidarTextoEAutorDoComentario() throws Exception {
        mockMvc.perform(post("/chamados/{id}/comentarios", chamado.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"texto":" ","autor":""}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void deveRetornarNaoEncontradoAoComentarChamadoInexistente() throws Exception {
        mockMvc.perform(post("/chamados/{id}/comentarios", Long.MAX_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"texto":"Comentário","autor":"Técnico"}
                    """))
            .andExpect(status().isNotFound());
    }
}
