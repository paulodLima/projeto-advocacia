package com.advocacia_microservice.usuario.infrastructure.persistence;

import com.advocacia_microservice.usuario.domain.UsuarioPerfil;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "usuario_perfil")
public class UsuarioPerfilEntity {
    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;
    @Column(nullable = false, length = 30)
    private String telefone;
    @Column(name = "email_pessoal", nullable = false, length = 254)
    private String emailPessoal;
    @Column(nullable = false, length = 500)
    private String endereco;
    @Column(nullable = false, columnDefinition = "text")
    private String foto;

    protected UsuarioPerfilEntity() {}

    public static UsuarioPerfilEntity de(UsuarioPerfil perfil) {
        var entity = new UsuarioPerfilEntity();
        entity.usuarioId = perfil.usuarioId();
        entity.telefone = perfil.telefone();
        entity.emailPessoal = perfil.emailPessoal();
        entity.endereco = perfil.endereco();
        entity.foto = perfil.foto();
        return entity;
    }

    public UsuarioPerfil paraDominio() {
        return new UsuarioPerfil(usuarioId, telefone, emailPessoal, endereco, foto);
    }
}
