package com.advocacia_microservice.agenda.infrastructure.persistence.repository;
import com.advocacia_microservice.agenda.domain.repository.VinculosAgendaRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
@Component
public class InicializarVinculosAgenda implements ApplicationRunner {
 private final VinculosAgendaRepository repository;
 public InicializarVinculosAgenda(VinculosAgendaRepository repository){this.repository=repository;}
 public void run(ApplicationArguments args){repository.reindexar();}
}
