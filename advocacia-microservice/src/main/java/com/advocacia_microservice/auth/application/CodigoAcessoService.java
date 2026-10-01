package com.advocacia_microservice.auth.application;

import com.advocacia_microservice.auth.infrastructure.EmailCodigoSender;
import com.advocacia_microservice.auth.infrastructure.persistence.*;
import com.advocacia_microservice.usuario.domain.StatusUsuario;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CodigoAcessoService {
    private final JpaUsuarioRepository usuarios;
    private final CodigoAcessoRepository codigos;
    private final EmailCodigoSender sender;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public CodigoAcessoService(JpaUsuarioRepository usuarios, CodigoAcessoRepository codigos,
                               EmailCodigoSender sender, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.sender = sender;
        this.encoder = encoder;
    }

    @Transactional
    public UUID solicitar(String email) {
        var agora = Instant.now();
        email = email.strip().toLowerCase(Locale.ROOT);
        var usuario = usuarios.findByEmail(email)
                .map(entity -> entity.paraDominio()).orElse(null);
        // Contas inativas continuam bloqueadas; emails novos podem validar para cadastro.
        if (usuario != null && usuario.status() != StatusUsuario.ATIVO) {
            return UUID.randomUUID();
        }
        var codigo = codigos.bloquearPorEmail(email).orElseGet(CodigoAcessoEntity::new);
        if (codigo.enviadoEm != null && codigo.enviadoEm.plusSeconds(60).isAfter(agora)) {
            return UUID.randomUUID();
        }
        if (codigo.janelaInicio == null || codigo.janelaInicio.plus(Duration.ofHours(1)).isBefore(agora)) {
            codigo.janelaInicio = agora;
            codigo.enviosNaJanela = 0;
        }
        if (codigo.enviosNaJanela >= 5) {
            return UUID.randomUUID();
        }
        String valor = String.format("%06d", random.nextInt(1_000_000));
        if (codigo.id == null) codigo.id = usuario == null ? UUID.randomUUID() : usuario.id();
        codigo.email = email;
        codigo.desafioId = UUID.randomUUID();
        codigo.hash = encoder.encode(valor);
        codigo.expiraEm = agora.plus(Duration.ofMinutes(10));
        codigo.enviadoEm = agora;
        codigo.tentativas = 0;
        codigo.utilizado = false;
        codigo.enviosNaJanela++;
        codigos.saveAndFlush(codigo);
        sender.enviar(email, valor);
        return codigo.desafioId;
    }

    @Transactional
    public Optional<String> validar(UUID desafio, String valor) {
        var codigo = codigos.bloquearPorDesafio(desafio).orElse(null);
        if (codigo == null || codigo.utilizado || codigo.tentativas >= 5
                || !codigo.expiraEm.isAfter(Instant.now())) {
            return Optional.empty();
        }
        codigo.tentativas++;
        if (!encoder.matches(valor, codigo.hash)) {
            return Optional.empty();
        }
        var usuario = usuarios.findByEmail(codigo.email).map(entity -> entity.paraDominio());
        if (usuario.isPresent() && usuario.get().status() != StatusUsuario.ATIVO) {
            return Optional.empty();
        }
        codigo.utilizado = true;
        return Optional.of(codigo.email);
    }
}
