package com.advocacia_microservice.auth.api.response;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record CsrfResponse(String token, String headerName) {}
