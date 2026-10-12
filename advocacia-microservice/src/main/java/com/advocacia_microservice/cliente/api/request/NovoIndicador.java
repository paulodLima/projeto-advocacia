package com.advocacia_microservice.cliente.api.request;

import com.advocacia_microservice.cliente.application.ContatosService;
import com.advocacia_microservice.cliente.domain.Contato;
import java.util.UUID;
import org.springframework.http.*;

public record NovoIndicador(String nome) {}
