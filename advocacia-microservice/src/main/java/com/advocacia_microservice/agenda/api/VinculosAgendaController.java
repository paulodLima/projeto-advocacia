package com.advocacia_microservice.agenda.api;
import com.advocacia_microservice.agenda.application.VinculosAgendaService;
import com.advocacia_microservice.agenda.domain.*;
import com.advocacia_microservice.agenda.api.response.PaginaVinculosResponse;
import java.time.LocalDate;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/agenda/vinculos")
public class VinculosAgendaController{
 private final VinculosAgendaService service;
 public VinculosAgendaController(VinculosAgendaService service){this.service=service;}
 @GetMapping public ResponseEntity<PaginaVinculosResponse> listar(Principal p,@RequestParam LocalDate inicio,@RequestParam LocalDate fim,
 @RequestParam(required=false) TipoAtividade tipo,@RequestParam(required=false) SituacaoAgenda situacao,@RequestParam(required=false) UUID responsavel,
 @RequestParam(defaultValue="") String busca,@RequestParam(defaultValue="true") boolean incluirSemData,
 @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="50") int tamanho){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PaginaVinculosResponse.de(service.listar(UUID.fromString(p.getName()),new FiltroAgenda(inicio,fim,tipo,situacao,responsavel,busca,incluirSemData),pagina,tamanho)));
 }
}
