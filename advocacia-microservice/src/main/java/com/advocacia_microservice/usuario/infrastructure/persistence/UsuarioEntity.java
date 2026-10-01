package com.advocacia_microservice.usuario.infrastructure.persistence;

import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.domain.Usuario;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
public class UsuarioEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status;

    protected UsuarioEntity() {}

    public static UsuarioEntity de(Usuario usuario) {
        var entity = new UsuarioEntity();
        entity.id = usuario.id();
        entity.nome = usuario.nome();
        entity.email = usuario.email();
        entity.status = usuario.status();
        return entity;
    }

    public Usuario paraDominio() {
        return new Usuario(id, nome, email, status);
    }
}
