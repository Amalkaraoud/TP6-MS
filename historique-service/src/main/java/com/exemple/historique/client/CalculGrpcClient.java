package com.exemple.historique.client;

import com.exemple.calcul.grpc.CalculGrpc;
import com.exemple.calcul.grpc.CalculReponse;
import com.exemple.calcul.grpc.CalculRequest;
import io.grpc.ManagedChannel;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Client gRPC de calcul-service : trouve les instances dans l'annuaire Eureka,
// les sollicite a tour de role et reutilise un canal HTTP/2 par instance.
// C'est, ecrit a la main, ce que @FeignClient + LoadBalancer font pour un appel REST.
@Service
public class CalculGrpcClient {

    private static final String SERVICE = "calcul-service"; // nom dans l'annuaire

    private final DiscoveryClient discoveryClient;   // acces a la copie locale de l'annuaire
    private final GrpcChannelFactory channelFactory; // fabrique de canaux (Spring gRPC)

    // Un canal par adresse "hote:port", cree a la premiere utilisation puis reutilise
    private final Map<String, ManagedChannel> canaux = new ConcurrentHashMap<>();

    // Compteur du tour de role (round robin)
    private final AtomicInteger compteur = new AtomicInteger();

    public CalculGrpcClient(DiscoveryClient discoveryClient, GrpcChannelFactory channelFactory) {
        this.discoveryClient = discoveryClient;
        this.channelFactory = channelFactory;
    }

    // Demande le calcul a UNE instance de calcul-service, en gRPC.
    // Leve StatusRuntimeException (erreur gRPC) ou IllegalStateException (aucune instance).
    public CalculReponse calculer(String operateur, int a, int b) {
        // 1. Les instances connues de l'annuaire (copie locale renouvelee toutes les 5 s)
        List<ServiceInstance> instances = new ArrayList<>(discoveryClient.getInstances(SERVICE));
        if (instances.isEmpty()) {
            throw new IllegalStateException("Aucune instance de " + SERVICE + " dans l'annuaire");
        }

        // 2. Tour de role : instance 0, 1, 0, 1...
        // L'ordre de la liste recue de l'annuaire n'est pas garanti stable d'un appel a l'autre :
        // sans tri, le compteur pourrait retomber deux fois de suite sur la meme instance.
        instances.sort(Comparator.comparing(ServiceInstance::getHost)
                .thenComparingInt(ServiceInstance::getPort));
        ServiceInstance instance = instances.get(
                Math.floorMod(compteur.getAndIncrement(), instances.size()));

        // 3. Le port gRPC vient des metadonnees publiees par calcul-service
        String portGrpc = instance.getMetadata().getOrDefault("grpc.port", "9090");
        String adresse = instance.getHost() + ":" + portGrpc;

        // 4. Le canal vers cette instance (cree une seule fois par adresse)
        ManagedChannel canal = canaux.computeIfAbsent(adresse, channelFactory::createChannel);

        // 5. Le stub bloquant : l'appel distant comme une methode locale
        CalculRequest requete = CalculRequest.newBuilder()
                .setOperateur(operateur).setA(a).setB(b).build();

        System.out.println("[historique-service] gRPC -> " + adresse + " : " + operateur);
        return CalculGrpc.newBlockingStub(canal).calculer(requete);
    }
}
