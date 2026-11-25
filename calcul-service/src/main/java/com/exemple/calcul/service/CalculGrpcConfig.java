package com.exemple.calcul.service;

import io.grpc.Status;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

// Traduit les exceptions Java levees par les services gRPC en statuts gRPC,
// comme un @ControllerAdvice traduit les exceptions en statuts HTTP.
@Configuration
public class CalculGrpcConfig {

    @Bean
    GrpcExceptionHandler calculExceptionHandler() {
        return exception -> {
            if (exception instanceof ArithmeticException
                    || exception instanceof IllegalArgumentException) {
                // Erreur due a la requete du client : INVALID_ARGUMENT (~ 400)
                return Status.INVALID_ARGUMENT
                        .withDescription("Erreur : " + exception.getMessage())
                        .asException();
            }
            return null; // autre exception : traitement par defaut (UNKNOWN)
        };
    }
}
