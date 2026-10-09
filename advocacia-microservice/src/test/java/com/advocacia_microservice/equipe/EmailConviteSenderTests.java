package com.advocacia_microservice.equipe;

import com.advocacia_microservice.equipe.infrastructure.EmailConviteSender;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailConviteSenderTests {
    @Test void preparaTextoHtmlEscapadoEUrlDeLoginSemEnviarEmailReal() throws Exception {
        var mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenAnswer(call -> new JavaMailSenderImpl().createMimeMessage());
        new EmailConviteSender(mail, "acesso@local.test", "http://localhost:4200").enviar("destino@example.test", "<script>Pessoa</script>", "Empresa & Sociedade");
        var captor = ArgumentCaptor.forClass(MimeMessage.class); verify(mail).send(captor.capture());
        var mensagem = captor.getValue(); mensagem.saveChanges();
        assertTrue(mensagem.getSubject().contains("Convite"));
        var html = encontrar(mensagem.getContent(), "text/html"); var texto = encontrar(mensagem.getContent(), "text/plain");
        assertNotNull(html); assertNotNull(texto);
        assertFalse(html.contains("<script>")); assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("http://localhost:4200/login")); assertTrue(html.contains("Empresa &amp; Sociedade"));
        assertTrue(texto.contains("7 dias")); assertFalse(html.contains("{{NOME}}"));
    }
    private String encontrar(Object content, String tipo) throws Exception {
        if (!(content instanceof Multipart multipart)) return null;
        for (int i = 0; i < multipart.getCount(); i++) { var parte = multipart.getBodyPart(i); if (parte.isMimeType(tipo)) return (String) parte.getContent(); var valor = encontrar(parte.getContent(), tipo); if (valor != null) return valor; }
        return null;
    }
}
