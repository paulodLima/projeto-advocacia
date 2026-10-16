package com.advocacia_microservice.caso.api;

import com.advocacia_microservice.caso.application.TarefasCasoService;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/casos/{caso}/tarefas")
public class TarefasCasoController {
    private final TarefasCasoService service;
    public TarefasCasoController(TarefasCasoService service){this.service=service;}
    private UUID usuario(Principal p){return UUID.fromString(p.getName());}
    @ModelAttribute public void cache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/opcoes") public Object opcoes(Principal p,@PathVariable UUID caso){return service.opcoes(usuario(p),caso);}
    @GetMapping public Object listar(Principal p,@PathVariable UUID caso){return service.listar(usuario(p),caso);}
    @GetMapping("/{id}") public Object buscar(Principal p,@PathVariable UUID caso,@PathVariable UUID id){return service.buscar(usuario(p),caso,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Object criar(Principal p,@PathVariable UUID caso,@RequestBody TarefasCasoService.Formulario f){return service.salvar(usuario(p),caso,null,f);}
    @PutMapping("/{id}") public Object salvar(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody TarefasCasoService.Formulario f){return service.salvar(usuario(p),caso,id,f);}
    @PutMapping("/{id}/estado") public Object estado(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody TarefasCasoService.Atualizacao f){return service.estado(usuario(p),caso,id,f);}
    @PutMapping("/{id}/adiamento") public Object adiar(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody TarefasCasoService.Adiamento f){return service.adiar(usuario(p),caso,id,f);}
    @PutMapping("/{id}/responsaveis") public Object delegar(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody TarefasCasoService.Delegacao f){return service.delegar(usuario(p),caso,id,f);}
    @PostMapping("/{id}/comentarios") public Object comentar(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestBody TarefasCasoService.ComentarioRequest f){return service.comentar(usuario(p),caso,id,f);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(Principal p,@PathVariable UUID caso,@PathVariable UUID id,@RequestParam int versao){service.excluir(usuario(p),caso,id,versao);}
}
