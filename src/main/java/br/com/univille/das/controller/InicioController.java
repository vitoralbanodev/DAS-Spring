package br.com.univille.das.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class InicioController {
    @GetMapping ("/ola")
    public String ola() {
        return "Spring Boot, Funcionando";
    }
}