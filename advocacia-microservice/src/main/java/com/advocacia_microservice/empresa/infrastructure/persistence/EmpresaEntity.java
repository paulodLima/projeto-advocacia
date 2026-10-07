package com.advocacia_microservice.empresa.infrastructure.persistence;

import com.advocacia_microservice.empresa.domain.Empresa;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "empresa")
public class EmpresaEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 32) private String cnpj;
    @ElementCollection
    @CollectionTable(name = "empresa_dado", joinColumns = @JoinColumn(name = "empresa_id"))
    @MapKeyColumn(name = "campo", length = 40)
    @Column(name = "valor", nullable = false, length = 1000)
    private Map<String, String> dados = new HashMap<>();
    protected EmpresaEntity() {}
    public static EmpresaEntity de(Empresa empresa) {
        var entity = new EmpresaEntity();
        entity.id = empresa.id();
        entity.cnpj = empresa.dados().get("cnpj");
        entity.dados = new HashMap<>(empresa.dados());
        return entity;
    }
    public Empresa paraDominio() { return new Empresa(id, dados); }
}
