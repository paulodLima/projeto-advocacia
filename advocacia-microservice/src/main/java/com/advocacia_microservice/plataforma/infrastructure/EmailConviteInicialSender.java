package com.advocacia_microservice.plataforma.infrastructure;

import com.advocacia_microservice.plataforma.domain.repository.NotificacaoConviteInicial;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailConviteInicialSender implements NotificacaoConviteInicial {
    private final JavaMailSender mail;
    private final String remetente;
    private final String url;
    public EmailConviteInicialSender(JavaMailSender mail, @Value("${app.auth.mail-from}") String remetente,
                                    @Value("${app.auth.public-url}") String url) {
        this.mail = mail; this.remetente = remetente; this.url = url.replaceAll("/+$", "") + "/login";
    }
    public void enviar(String email, String nome) {
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente); mensagem.setTo(email);
        mensagem.setSubject("Convite para cadastrar seu escritório — Gestão Advocacia");
        mensagem.setText("Olá, " + nome + ".\nVocê foi convidado para cadastrar seu escritório.\nEntre em " + url
                + " usando este email e o código de acesso ou Google. Após confirmar o email, complete seu cadastro, cadastre o escritório e convide os funcionários."
                + "\nO convite é pessoal, expira em 7 dias e não concede administração da plataforma.");
        mail.send(mensagem);
    }
}
