package com.exemple.historique.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.exemple.historique.config.HistoriqueParams;

// Expose la configuration recue. Pas de @RefreshScope : volontaire (E.3)
@RestController
@RequestMapping("/operations/config")
public class HistoriqueConfigController {

    private final HistoriqueParams params;        // bean @ConfigurationProperties

    @Value("${server.port}")
    private int port;

    @Value("${spring.profiles.active:default}")
    private String profil;

    @Value("${calculatrice.equipe}")
    private String equipe;

    public HistoriqueConfigController(HistoriqueParams params) {
        this.params = params;
    }

    @GetMapping
    public Map<String, Object> config() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("service", "historique-service");
        reponse.put("port", port);
        reponse.put("profil", profil);
        reponse.put("maxOperations", params.getMaxOperations());
        reponse.put("message", params.getMessage());
        reponse.put("equipe", equipe);
        return reponse;
    }
}
