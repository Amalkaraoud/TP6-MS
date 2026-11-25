package com.exemple.historique.controller;

import com.exemple.calcul.grpc.CalculReponse;
import com.exemple.historique.client.CalculGrpcClient;
import com.exemple.historique.config.HistoriqueParams;
import com.exemple.historique.model.Operation;
import com.exemple.historique.service.OperationService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/operations")
public class OperationController {

    private static final List<String> OPERATEURS =
            List.of("somme", "soustraction", "produit", "division");

    private final OperationService operationService;
    private final CalculGrpcClient calculGrpcClient; // TP6 : gRPC (remplace OpenFeign)
    private final HistoriqueParams params; // proprietes historique.*

    public OperationController(OperationService operationService,
                               CalculGrpcClient calculGrpcClient,
                               HistoriqueParams params) {
        this.operationService = operationService;
        this.calculGrpcClient = calculGrpcClient;
        this.params = params;
    }

    // Demande le resultat a calcul-service en gRPC (instance choisie par
    // CalculGrpcClient) et le range dans l'operation. Retourne une reponse
    // d'erreur si le calcul est impossible, null sinon.
    private ResponseEntity<Object> calculer(Operation op) {
        if (op.getOperateur() == null || !OPERATEURS.contains(op.getOperateur())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("erreur", "Operateur inconnu : " + op.getOperateur()));
        }

        try {
            CalculReponse reponse = calculGrpcClient.calculer(op.getOperateur(),
                    op.getA(), op.getB());
            op.setResultat(reponse.getResultat()); // double, sans parseDouble
            return null;

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                // calcul-service a refuse la requete (division par 0...) :
                // on transmet sa description, comme le 400 d'un appel REST
                return ResponseEntity.badRequest()
                        .body(Map.of("erreur", e.getStatus().getDescription()));
            }
            // UNAVAILABLE, DEADLINE_EXCEEDED, UNKNOWN... : instance injoignable ou en erreur
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erreur", "calcul-service injoignable (gRPC "
                            + e.getStatus().getCode() + ")"));

        } catch (IllegalStateException e) {
            // Aucune instance de calcul-service dans l'annuaire
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("erreur", e.getMessage()));
        }
    }

    // POST /operations : crée une opération
    @PostMapping
    public ResponseEntity<Object> creer(@RequestBody Operation op) {
        // refuse l'operation si l'historique a atteint la taille configuree
        if (operationService.lister().size() >= params.getMaxOperations()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("erreur", "Historique plein : "
                            + params.getMaxOperations() + " operations au maximum"));
        }

        ResponseEntity<Object> erreur = calculer(op);

        if (erreur != null) {
            return erreur;
        }

        Operation creee = operationService.ajouter(op);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creee.getId())
                .toUri();

        return ResponseEntity.created(location).body(creee);
    }

    // GET /operations : retourne toutes les opérations
    @GetMapping
    public List<Operation> lister() {
        return operationService.lister();
    }

    // GET /operations/{id} : retourne une opération
    @GetMapping("/{id}")
    public ResponseEntity<Operation> lire(@PathVariable("id") int id) {
        Operation op = operationService.lire(id);

        if (op == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(op);
    }

    // PUT /operations/{id} : modifie une opération
    @PutMapping("/{id}")
    public ResponseEntity<Object> modifier(@PathVariable("id") int id,
                                           @RequestBody Operation op) {
        if (!operationService.existe(id)) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<Object> erreur = calculer(op);

        if (erreur != null) {
            return erreur;
        }

        operationService.remplacer(id, op);
        return ResponseEntity.ok(op);
    }

    // DELETE /operations/{id} : supprime une opération
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable("id") int id) {
        if (operationService.supprimer(id) == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
