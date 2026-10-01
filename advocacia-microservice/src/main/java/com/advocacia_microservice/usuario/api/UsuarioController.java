package com.advocacia_microservice.usuario.api;

import com.advocacia_microservice.usuario.api.request.AtualizarUsuarioRequest;
import com.advocacia_microservice.usuario.api.request.CriarUsuarioRequest;
import com.advocacia_microservice.usuario.api.response.UsuarioResponse;
import com.advocacia_microservice.usuario.application.AtualizarUsuarioUseCase;
import com.advocacia_microservice.usuario.application.BuscarUsuarioUseCase;
import com.advocacia_microservice.usuario.application.CriarUsuarioUseCase;
import com.advocacia_microservice.usuario.application.ExcluirUsuarioUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final CriarUsuarioUseCase criar;
    private final AtualizarUsuarioUseCase atualizar;
    private final BuscarUsuarioUseCase buscar;
    private final ExcluirUsuarioUseCase excluir;

    public UsuarioController(CriarUsuarioUseCase criar, AtualizarUsuarioUseCase atualizar,
                             BuscarUsuarioUseCase buscar, ExcluirUsuarioUseCase excluir) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.buscar = buscar;
        this.excluir = excluir;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody CriarUsuarioRequest request) {
        var usuario = criar.executar(request.nome(), request.email());
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id()))
                .body(UsuarioResponse.de(usuario));
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscar(@PathVariable UUID id) {
        return UsuarioResponse.de(buscar.executar(id));
    }

    @PutMapping("/{id}")
    public UsuarioResponse atualizar(@PathVariable UUID id,
                                    @Valid @RequestBody AtualizarUsuarioRequest request) {
        return UsuarioResponse.de(atualizar.executar(id, request.nome(), request.email(), request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable UUID id) {
        excluir.executar(id);
        return ResponseEntity.noContent().build();
    }
}
