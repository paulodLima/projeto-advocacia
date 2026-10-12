package com.advocacia_microservice.auth.application;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;

@Service
public class ProvedoresLoginService {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    public ProvedoresLoginService(ObjectProvider<ClientRegistrationRepository> registrations) { this.registrations = registrations; }
    public boolean googleDisponivel() { return registrations.getIfAvailable() != null; }
}
