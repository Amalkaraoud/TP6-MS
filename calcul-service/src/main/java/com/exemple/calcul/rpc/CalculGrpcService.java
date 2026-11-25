package com.exemple.calcul.rpc;

import com.exemple.calcul.grpc.CalculGrpc;
import com.exemple.calcul.grpc.CalculReponse;
import com.exemple.calcul.grpc.CalculRequest;
import com.exemple.calcul.service.Calculatrice;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.grpc.server.service.GrpcService;

// Le service gRPC : etend la classe abstraite generee et delegue a Calculatrice,
// exactement comme CalculController (REST). Le controleur traduit HTTP -> Java,
// cette classe traduit gRPC -> Java. La logique metier n'est ecrite qu'une fois.
@GrpcService
public class CalculGrpcService extends CalculGrpc.CalculImplBase {

    private final Calculatrice calculService;
    private final Environment env;

    // Port HTTP de cette instance : renvoye dans la reponse et dans la trace
    @Value("${server.port}")
    private int port;

    public CalculGrpcService(Calculatrice calculService, Environment env) {
        this.calculService = calculService;
        this.env = env;
    }

    // Limite calcul.max-operande du depot de configuration, relue a
    // CHAQUE appel dans l'environnement : un POST /actuator/refresh suffit,
    // sans @RefreshScope (un proxy empecherait l'enregistrement du service gRPC)
    private void verifierLimite(int a, int b) {
        int max = env.getProperty("calcul.max-operande", Integer.class, Integer.MAX_VALUE);
        if (Math.abs(a) > max || Math.abs(b) > max) {
            throw new IllegalArgumentException("operande hors limite (max " + max + ")");
        }
    }

    @Override
    public void calculer(CalculRequest request,
                         StreamObserver<CalculReponse> responseObserver) {
        String op = request.getOperateur();
        int a = request.getA();
        int b = request.getB();

        verifierLimite(a, b);
        System.out.println("[calcul-service:" + port + "] gRPC " + op + " " + a + "," + b);

        double resultat = switch (op) {
            case "somme" -> calculService.somme(a, b);
            case "soustraction" -> calculService.soustraction(a, b);
            case "produit" -> calculService.produit(a, b);
            case "division" -> calculService.division(a, b); // ArithmeticException si b = 0
            default -> throw new IllegalArgumentException("Operateur inconnu : " + op);
        };

        CalculReponse reponse = CalculReponse.newBuilder()
                .setOperateur(op).setA(a).setB(b)
                .setResultat(resultat)
                .setPort(port)
                .build();

        responseObserver.onNext(reponse);   // UNE reponse (appel unaire)...
        responseObserver.onCompleted();     // ... puis fin de l'appel
    }

    // E.3 - flux serveur : UNE requete, N reponses (table de multiplication de a, de 1 a b)
    @Override
    public void table(CalculRequest request, StreamObserver<CalculReponse> responseObserver) {
        int a = request.getA();
        verifierLimite(a, request.getB());
        System.out.println("[calcul-service:" + port + "] gRPC table " + a + " x 1.." + request.getB());
        for (int i = 1; i <= request.getB(); i++) {
            responseObserver.onNext(CalculReponse.newBuilder()
                    .setOperateur("produit").setA(a).setB(i)
                    .setResultat(calculService.produit(a, i)).setPort(port).build());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        responseObserver.onCompleted();      // N onNext, puis UN onCompleted
    }
}
