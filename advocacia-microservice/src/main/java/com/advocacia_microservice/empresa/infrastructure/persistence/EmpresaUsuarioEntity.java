package com.advocacia_microservice.empresa.infrastructure.persistence;

import com.advocacia_microservice.empresa.domain.*;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "empresa_usuario")
public class EmpresaUsuarioEntity {
    @Id @Column(name = "usuario_id") private UUID usuarioId;
    @Column(name = "empresa_id", nullable = false) private UUID empresaId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PapelEmpresa papel;
    protected EmpresaUsuarioEntity() {}
    public static EmpresaUsuarioEntity de(VinculoEmpresa vinculo) {
        var entity = new EmpresaUsuarioEntity();
        entity.usuarioId = vinculo.usuarioId(); entity.empresaId = vinculo.empresaId(); entity.papel = vinculo.papel();
        return entity;
    }
    public VinculoEmpresa paraDominio() { return new VinculoEmpresa(usuarioId, empresaId, papel); }
}
