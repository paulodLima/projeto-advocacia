package com.advocacia_microservice.cofre.application;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class CofreCrypto {
    private final SecretKeySpec chave;
    private final SecureRandom random=new SecureRandom();
    public CofreCrypto(@Value("${COFRE_KEY:}") String valor) {
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(valor); } catch(IllegalArgumentException e) { bytes=new byte[0]; }
        chave=bytes.length==32 ? new SecretKeySpec(bytes,"AES") : null;
    }
    public boolean configurado() { return chave!=null; }
    public String cifrar(UUID empresa,UUID id,byte[] texto) { return Base64.getEncoder().encodeToString(transformar(true,empresa,id,texto)); }
    public byte[] decifrar(UUID empresa,UUID id,String texto) {
        try { return transformar(false,empresa,id,Base64.getDecoder().decode(texto)); }
        catch(IllegalArgumentException e) { throw indisponivel(); }
    }
    private byte[] transformar(boolean criar,UUID empresa,UUID id,byte[] dados) {
        if(chave==null) throw indisponivel();
        try {
            byte[] nonce;
            if(criar) { nonce=new byte[12];random.nextBytes(nonce); }
            else { if(dados.length<28) throw new IllegalArgumentException(); nonce=Arrays.copyOfRange(dados,0,12); }
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(criar ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,chave,new GCMParameterSpec(128,nonce));
            cipher.updateAAD(("cofre-v1:"+empresa+":"+id).getBytes(StandardCharsets.UTF_8));
            byte[] resultado=cipher.doFinal(criar ? dados : Arrays.copyOfRange(dados,12,dados.length));
            if(!criar) return resultado;
            byte[] completo=new byte[nonce.length+resultado.length];System.arraycopy(nonce,0,completo,0,nonce.length);System.arraycopy(resultado,0,completo,nonce.length,resultado.length);return completo;
        } catch(Exception e) { throw indisponivel(); }
    }
    private ResponseStatusException indisponivel() { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Cofre indisponível. Verifique a chave de criptografia com o administrador."); }
}
