package br.com.sistemas.chamados.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Lançada quando uma regra de negócio é violada (vira HTTP 409). */
public record ErroResposta(
    LocalDateTime timestamp,
    int status,
    String erro,
    List<String> detalhes
) { }
