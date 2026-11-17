package com.exemple.historique.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Regroupe les proprietes historique.* du depot de configuration :
//   historique.max-operations -> maxOperations (tirets -> camelCase)
//   historique.message         -> message
// Spring remplit les attributs par les setters (Lombok), et les re-remplit
// apres un refresh : aucun @RefreshScope necessaire.
@ConfigurationProperties(prefix = "historique")
@Getter @Setter @ToString
public class HistoriqueParams {
    private int maxOperations;
    private String message;
}
