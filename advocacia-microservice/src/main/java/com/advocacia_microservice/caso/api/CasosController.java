package com.advocacia_microservice.caso.api;
import com.advocacia_microservice.caso.application.CasosService;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
@RestController @RequestMapping("/api/casos")
public class CasosController {
    private final CasosService service;
    public CasosController(CasosService service){this.service=service;}
    private UUID usuario(Principal p){return UUID.fromString(p.getName());}
    @ModelAttribute public void cache(HttpServletResponse response){response.setHeader("Cache-Control","no-store");}
    @GetMapping("/opcoes") public CasosService.Opcoes opcoes(Principal p){return service.opcoes(usuario(p));}
    @GetMapping public CasosService.Pagina listar(Principal p,@RequestParam(defaultValue="")String busca,@RequestParam(defaultValue="ativo")String status,@RequestParam(defaultValue="0")int pagina,@RequestParam(defaultValue="24")int tamanho){return service.listar(usuario(p),busca,status,pagina,tamanho);}
    @GetMapping("/{id}") public CasosService.Ficha buscar(Principal p,@PathVariable UUID id){return service.buscar(usuario(p),id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CasosService.Ficha criar(Principal p,@RequestBody CasosService.Formulario f){return service.salvar(usuario(p),null,f);}
    @PutMapping("/{id}") public CasosService.Ficha salvar(Principal p,@PathVariable UUID id,@RequestBody CasosService.Formulario f){return service.salvar(usuario(p),id,f);}
    @PutMapping("/{id}/comercial") public CasosService.Ficha comercial(Principal p,@PathVariable UUID id,@RequestBody CasosService.Comercial f){return service.comercial(usuario(p),id,f);}
    @PutMapping("/{id}/situacao") public CasosService.Ficha situacao(Principal p,@PathVariable UUID id,@RequestBody CasosService.SituacaoRequest f){return service.situacao(usuario(p),id,f);}
    @PostMapping("/{id}/anotacoes") public CasosService.Ficha anotar(Principal p,@PathVariable UUID id,@RequestBody CasosService.AnotacaoRequest f){return service.anotar(usuario(p),id,f);}
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void excluir(Principal p,@PathVariable UUID id,@RequestParam int versao){service.excluir(usuario(p),id,versao);}
}
