package videoservice;

import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;


public class ServerVideos {
    public static void main(String[] args){



        int port = 8080;

        try {

        Server server = ServerBuilder
                .forPort(port)
                .addService(new ServerImplementation())
                .intercept(new AuthTokenInterceptor())
                .addService(ProtoReflectionService.newInstance())
                .build();

        server.start();

        System.out.println("Server starting...");
        System.out.println("Listening on port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Receiving shutdown request...");
                server.shutdown();
                System.out.println("Server stopped");
            }
        });

        server.awaitTermination();

        } catch (IOException e) {
            System.out.println("Error starting server: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Server interrupted: " + e.getMessage());
        }
    }
}