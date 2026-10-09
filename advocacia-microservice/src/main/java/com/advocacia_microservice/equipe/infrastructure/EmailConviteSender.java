package com.advocacia_microservice.equipe.infrastructure;

import jakarta.mail.MessagingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class EmailConviteSender {
    private final JavaMailSender mail;
    private final String remetente;
    private final String url;
    private final String template;
    public EmailConviteSender(JavaMailSender mail, @Value("${app.auth.mail-from}") String remetente,
            @Value("${app.auth.public-url}") String url) {
        this.mail = mail; this.remetente = remetente; this.url = url.replaceAll("/+$", "") + "/login";
        try (var stream = new ClassPathResource("templates/email/convite-equipe.html").getInputStream()) {
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException erro) { throw new MailPreparationException("Não foi possível carregar o convite.", erro); }
    }
    public void enviar(String email, String nome, String empresa) {
        try {
            var helper = new MimeMessageHelper(mail.createMimeMessage(), true, StandardCharsets.UTF_8.name());
            helper.setFrom(remetente, "Gestão Advocacia"); helper.setTo(email);
            helper.setSubject("Convite para a equipe — Gestão Advocacia");
            helper.setText("Olá, " + nome + ".\nVocê foi convidado para " + empresa
                    + ".\nEntre em " + url + " com este e-mail, usando código de acesso ou Google."
                    + "\nO vínculo será criado após validar seu e-mail. O convite expira em 7 dias.",
                    template.replace("{{NOME}}", HtmlUtils.htmlEscape(nome)).replace("{{EMPRESA}}", HtmlUtils.htmlEscape(empresa)).replace("{{URL}}", HtmlUtils.htmlEscape(url)));
            mail.send(helper.getMimeMessage());
        } catch (MessagingException | IOException erro) { throw new MailPreparationException("Não foi possível preparar o convite.", erro); }
    }
}
