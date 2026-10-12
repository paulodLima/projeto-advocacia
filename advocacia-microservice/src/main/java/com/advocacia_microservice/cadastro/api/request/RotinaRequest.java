package com.advocacia_microservice.cadastro.api.request;

import com.advocacia_microservice.cadastro.application.CadastrosService;
import com.advocacia_microservice.cadastro.domain.*;
import com.advocacia_microservice.cadastro.infrastructure.CadastrosRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;

public record RotinaRequest(String nome, String periodo) {}
