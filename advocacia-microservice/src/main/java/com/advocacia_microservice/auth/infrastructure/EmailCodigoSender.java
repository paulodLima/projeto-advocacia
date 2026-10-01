package com.advocacia_microservice.auth.infrastructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailCodigoSender {
    private final JavaMailSender mail;
    private final String remetente;
    private final String template;

    public EmailCodigoSender(JavaMailSender mail, @Value("${app.auth.mail-from}") String remetente) {
        this.mail = mail;
        this.remetente = remetente;
        try (var stream = new ClassPathResource("templates/email/codigo-acesso.html").getInputStream()) {
            this.template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new MailPreparationException("Não foi possível carregar o modelo de email.", exception);
        }
    }

    public void enviar(String email, String codigo) {
        if (codigo == null || !codigo.matches("[0-9]{6}")) {
            throw new IllegalArgumentException("Código de acesso deve conter seis dígitos.");
        }
        try {
            var mensagem = mail.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente, "Gestão Advocacia");
            helper.setTo(email);
            helper.setSubject("Seu código de acesso — Gestão Advocacia");
            helper.setText("Gestão Advocacia\n\nSeu código de acesso é: " + codigo
                    + "\n\nUse este código na tela em que solicitou o acesso."
                    + "\nEle expira em 10 minutos e pode ser usado apenas uma vez. Não compartilhe este código."
                    + "\nSe você não solicitou o acesso, ignore este email.",
                    template.replace("{{CODIGO}}", codigo));
            mail.send(mensagem);
        } catch (MessagingException | IOException exception) {
            throw new MailPreparationException("Não foi possível preparar o email de acesso.", exception);
        }
    }
}
