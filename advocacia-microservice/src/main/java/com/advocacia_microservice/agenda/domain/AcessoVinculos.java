package com.advocacia_microservice.agenda.domain;
import java.util.UUID;
public record AcessoVinculos(UUID empresa,UUID usuario,boolean crm,boolean casos,boolean master) {}
