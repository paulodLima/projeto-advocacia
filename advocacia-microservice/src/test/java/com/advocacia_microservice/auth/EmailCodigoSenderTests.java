package com.advocacia_microservice.auth;

import com.advocacia_microservice.auth.infrastructure.EmailCodigoSender;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailCodigoSenderTests {
    @Test
    void enviaAlternativasTextoEHtmlComCodigoEIdentidade() throws Exception {
        var mail = mock(JavaMailSender.class);
        when(mail.createMimeMessage()).thenAnswer(call -> new JavaMailSenderImpl().createMimeMessage());
        new EmailCodigoSender(mail, "acesso@local.test").enviar("destino@example.test", "012345");
        var captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mail).send(captor.capture());
        var message = captor.getValue();
        message.saveChanges();
        assertEquals("Gestão Advocacia", ((InternetAddress) message.getFrom()[0]).getPersonal());
        assertEquals("destino@example.test", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        assertEquals("Seu código de acesso — Gestão Advocacia", message.getSubject());
        var plain = encontrar(message.getContent(), "text/plain");
        var html = encontrar(message.getContent(), "text/html");
        assertNotNull(plain);
        assertNotNull(html);
        assertTrue(plain.contains("012345"));
        assertTrue(plain.contains("10 minutos"));
        assertTrue(html.contains("012345"));
        assertTrue(html.contains("Gestão Advocacia"));
        assertTrue(html.contains("#4f5a49"));
        assertFalse(html.contains("{{CODIGO}}"));
    }

    private String encontrar(Object content, String tipo) throws Exception {
        if (!(content instanceof Multipart multipart)) return null;
        for (int i = 0; i < multipart.getCount(); i++) {
            var part = multipart.getBodyPart(i);
            if (part.isMimeType(tipo)) return (String) part.getContent();
            var nested = encontrar(part.getContent(), tipo);
            if (nested != null) return nested;
        }
        return null;
    }
}
