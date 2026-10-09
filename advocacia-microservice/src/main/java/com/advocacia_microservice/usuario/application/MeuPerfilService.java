package com.advocacia_microservice.usuario.application;

import com.advocacia_microservice.shared.exception.RecursoNaoEncontradoException;
import com.advocacia_microservice.usuario.domain.*;
import com.advocacia_microservice.usuario.infrastructure.persistence.JpaUsuarioRepository;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeuPerfilService {
    private final UsuarioPerfilRepository perfis;
    private final JpaUsuarioRepository usuarios;
    public MeuPerfilService(UsuarioPerfilRepository perfis, JpaUsuarioRepository usuarios) {
        this.perfis = perfis;
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public UsuarioPerfil buscar(UUID id) {
        return perfis.buscarPorUsuario(id).orElseGet(() -> UsuarioPerfil.vazio(id));
    }

    @Transactional
    public UsuarioPerfil atualizar(UUID id, String telefone, String emailPessoal, String endereco) {
        bloquearUsuario(id);
        var atual = buscar(id);
        return perfis.salvar(new UsuarioPerfil(id, telefone.strip(), emailPessoal.strip().toLowerCase(Locale.ROOT), endereco.strip(), atual.foto()));
    }

    @Transactional
    public UsuarioPerfil atualizarFoto(UUID id, String foto) {
        validarFoto(foto);
        bloquearUsuario(id);
        var atual = buscar(id);
        return perfis.salvar(new UsuarioPerfil(id, atual.telefone(), atual.emailPessoal(), atual.endereco(), foto));
    }

    private void bloquearUsuario(UUID id) {
        usuarios.buscarParaAtualizar(id).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private void validarFoto(String foto) {
        if (foto.isEmpty()) return;
        if (foto.length() > 700_000 || !foto.matches("data:image/(jpeg|png);base64,[A-Za-z0-9+/=]+")) {
            throw new IllegalArgumentException("Envie uma foto JPEG ou PNG de até 512 × 512 pixels.");
        }
        try {
            var bytes = Base64.getDecoder().decode(foto.substring(foto.indexOf(',') + 1));
            if (bytes.length > 512 * 1024) throw new IllegalArgumentException("A foto deve ter até 512 KB após a redução.");
            // Consulta dimensões sem decodificar imagens grandes em memória.
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Foto inválida.");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    var formato = reader.getFormatName().toLowerCase(Locale.ROOT);
                    var esperado = foto.startsWith("data:image/png;") ? "png" : "jpeg";
                    if (!formato.equals(esperado) || reader.getWidth(0) > 512 || reader.getHeight(0) > 512) {
                        throw new IllegalArgumentException("Envie uma foto JPEG ou PNG de até 512 × 512 pixels.");
                    }
                    if (reader.read(0) == null) throw new IllegalArgumentException("Foto inválida.");
                } finally { reader.dispose(); }
            }
        } catch (java.io.IOException | IllegalArgumentException erro) {
            throw new IllegalArgumentException("Foto inválida. Envie JPEG ou PNG de até 512 × 512 pixels e 512 KB.");
        }
    }
}
