package com.advocacia_microservice.empresa.infrastructure.persistence;

import com.advocacia_microservice.empresa.domain.IdentidadeVisual;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "empresa_identidade")
public class IdentidadeVisualEntity {
    @Id @Column(name = "empresa_id") private UUID empresaId;
    private double zoom;
    @Column(name = "pos_x") private double posX;
    @Column(name = "pos_y") private double posY;
    @ElementCollection @CollectionTable(name = "empresa_identidade_imagem", joinColumns = @JoinColumn(name = "empresa_id"))
    @MapKeyColumn(name = "tipo", length = 40) @Column(name = "imagem", columnDefinition = "TEXT", nullable = false)
    private Map<String, String> imagens = new HashMap<>();
    @ElementCollection @CollectionTable(name = "empresa_identidade_uso", joinColumns = @JoinColumn(name = "empresa_id"))
    @MapKeyColumn(name = "local", length = 30) @Column(name = "tipo", length = 40, nullable = false)
    private Map<String, String> usos = new HashMap<>();
    protected IdentidadeVisualEntity() {}
    public static IdentidadeVisualEntity de(UUID empresaId, IdentidadeVisual valor) {
        var entity = new IdentidadeVisualEntity(); entity.empresaId = empresaId;
        entity.zoom = valor.zoom(); entity.posX = valor.posX(); entity.posY = valor.posY();
        entity.imagens = new HashMap<>(valor.imagens()); entity.usos = new HashMap<>(valor.usos()); return entity;
    }
    public IdentidadeVisual paraDominio() { return new IdentidadeVisual(imagens, usos, zoom, posX, posY); }
}
