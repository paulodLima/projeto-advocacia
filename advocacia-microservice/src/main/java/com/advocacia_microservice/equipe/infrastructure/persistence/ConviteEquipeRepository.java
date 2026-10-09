package com.advocacia_microservice.equipe.infrastructure.persistence;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface ConviteEquipeRepository extends JpaRepository<ConviteEquipeEntity, UUID> {
    List<ConviteEquipeEntity> findByEmpresaIdAndStatusOrderByNome(UUID empresaId, String status);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConviteEquipeEntity c where c.email = :email")
    Optional<ConviteEquipeEntity> buscarPorEmailParaAtualizar(@org.springframework.data.repository.query.Param("email") String email);
}
