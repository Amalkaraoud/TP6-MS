
package com.exemple.calcul.controller;

import com.exemple.calcul.model.Resultat;
import com.exemple.calcul.service.Calculatrice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
@RequestMapping("/calcul")
public class CalculController {

    private final Calculatrice calculService;

    @Value("${server.port}")
    private int port;

    // Valeur absolue maximale des operandes, lue dans le depot de configuration
    @Value("${calcul.max-operande}")
    private int maxOperande;

    // Retourne une reponse 400 si a ou b depasse la limite, null sinon
    private ResponseEntity<String> verifierLimite(int a, int b) {
        if (Math.abs(a) > maxOperande || Math.abs(b) > maxOperande) {
            return ResponseEntity.badRequest()
                    .body("Erreur : operande hors limite (max "
                            + maxOperande + ")");
        }
        return null;
    }

    // Injection par constructeur : Spring fournit l'objet Calculatrice
    public CalculController(Calculatrice calculService) {
        this.calculService = calculService;
    }

    @GetMapping(value = "/somme", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> somme(@RequestParam("a") int a,
                                        @RequestParam("b") int b) {
        ResponseEntity<String> erreur = verifierLimite(a, b);
        if (erreur != null) return erreur;
        System.out.println("[calcul-service:" + port + "] somme " + a + "+" + b);
        System.out.println("somme appelee : a=" + a + ", b=" + b);
        int result = calculService.somme(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }

    @GetMapping(value = "/soustraction", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> soustraction(@RequestParam("a") int a,
                                               @RequestParam("b") int b) {
        ResponseEntity<String> erreur = verifierLimite(a, b);
        if (erreur != null) return erreur;
        System.out.println("[calcul-service:" + port + "] soustraction " + a + "-" + b);
        int result = calculService.soustraction(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }

    @GetMapping(value = "/produit", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> produit(@RequestParam("a") int a,
                                          @RequestParam("b") int b) {
        ResponseEntity<String> erreur = verifierLimite(a, b);
        if (erreur != null) return erreur;
        System.out.println("[calcul-service:" + port + "] produit " + a + "*" + b);
        int result = calculService.produit(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }

    @GetMapping(value = "/division", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> division(@RequestParam("a") int a,
                                           @RequestParam("b") int b) {
        ResponseEntity<String> erreur = verifierLimite(a, b);
        if (erreur != null) return erreur;
        System.out.println("[calcul-service:" + port + "] division " + a + "/" + b);
        if (b == 0) {
            return ResponseEntity.badRequest()
                    .body("Erreur : division par 0 impossible");
        }
        double result = calculService.division(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }

    @GetMapping(value = "/produit/{a}/{b}", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> produitChemin(@PathVariable("a") int a,
                                                @PathVariable("b") int b) {
        int result = calculService.produit(a, b);
        return ResponseEntity.ok(String.valueOf(result));
    }

    @GetMapping("/somme/detail")
    public Resultat sommeDetail(@RequestParam("a") int a,
                                @RequestParam("b") int b) {
        return new Resultat("somme", a, b, calculService.somme(a, b));
    }

    @GetMapping("/division/detail")
    public ResponseEntity<Resultat> divisionDetail(@RequestParam("a") int a,
                                                   @RequestParam("b") int b) {
        if (b == 0) {
            return ResponseEntity.badRequest().build();
        }
        Resultat resultat = new Resultat("division", a, b, calculService.division(a, b));
        return ResponseEntity.ok(resultat);
    }
}
