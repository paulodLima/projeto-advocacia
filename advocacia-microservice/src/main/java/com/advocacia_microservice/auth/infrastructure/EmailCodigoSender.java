package com.advocacia_microservice.auth.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailCodigoSender {
    private final JavaMailSender mail;
    private final String remetente;

    public EmailCodigoSender(JavaMailSender mail, @Value("${app.auth.mail-from}") String remetente) {
        this.mail = mail;
        this.remetente = remetente;
    }

    public void enviar(String email, String codigo) {
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Seu código de acesso — Advocacia");
        mensagem.setText("Seu código de acesso é: " + codigo
                + "\n\nEle expira em 10 minutos e pode ser usado apenas uma vez."
                + "\nSe você não solicitou o acesso, ignore este email.");
        mail.send(mensagem);
    }
}
