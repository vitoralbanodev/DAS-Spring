package br.com.sistemas.chamados.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Comentario;
import br.com.sistemas.chamados.entity.Prioridade;

@DataJpaTest
class ComentarioRepositoryTest {

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Test
    void devePersistirComentarioVinculadoAoChamado() {
        Chamado chamado = criarChamado();

        Comentario salvo = comentarioRepository.saveAndFlush(
            new Comentario("Estou verificando o problema.", "Ana", chamado)
        );

        Comentario encontrado = comentarioRepository.findByChamado_IdOrderByDataAsc(chamado.getId()).getFirst();
        assertEquals("Estou verificando o problema.", encontrado.getTexto());
        assertEquals("Ana", encontrado.getAutor());
        assertEquals(chamado.getId(), encontrado.getChamado().getId());
        assertNotNull(salvo.getData());
    }

    @Test
    void deveImpedirEmailDuplicadoDeCliente() {
        clienteRepository.saveAndFlush(new Cliente("Cliente 1", "duplicado@teste.com", null));

        assertThrows(DataIntegrityViolationException.class,
            () -> clienteRepository.saveAndFlush(new Cliente("Cliente 2", "duplicado@teste.com", null)));
    }

    private Chamado criarChamado() {
        Cliente cliente = clienteRepository.saveAndFlush(
            new Cliente("Cliente teste", "cliente@teste.com", null)
        );
        Categoria categoria = categoriaRepository.saveAndFlush(
            new Categoria("Categoria teste", null)
        );
        return chamadoRepository.saveAndFlush(
            new Chamado("Chamado teste", "Descrição", Prioridade.MEDIA, cliente, categoria, null)
        );
    }
}
