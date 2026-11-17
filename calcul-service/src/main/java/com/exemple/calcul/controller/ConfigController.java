package com.exemple.calcul.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Expose la configuration recue par ce microservice.
// @RefreshScope : apres POST /actuator/refresh, le bean est recree
// et ses @Value sont relus
@RestController
@RequestMapping("/calcul/config")
@RefreshScope
public class ConfigController {

    // Proprietes venant du depot Git, via config-service
    @Value("${calcul.max-operande}")
    private int maxOperande;

    @Value("${calcul.message}")
    private String message;

    @Value("${calculatrice.equipe}")
    private String equipe;

    // Proprietes locales (application.properties du module, ligne de commande)
    @Value("${server.port}")
    private int port;

    // Profil actif ; "default" si aucun n'est active (valeur apres le :)
    @Value("${spring.profiles.active:default}")
    private String profil;

    @GetMapping
    public Map<String, Object> config() {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("service", "calcul-service");
        reponse.put("port", port);
        reponse.put("profil", profil);
        reponse.put("maxOperande", maxOperande);
        reponse.put("message", message);
        reponse.put("equipe", equipe);
        return reponse;
    }
}
