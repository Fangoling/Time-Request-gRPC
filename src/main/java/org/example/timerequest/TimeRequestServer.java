package org.example.timerequest;

import com.example.time.protos.TimeGrpc;
import com.example.time.protos.TimeItem;
import com.example.time.protos.TimeRequestItem;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TimeRequestServer {

    private static final Logger logger = Logger.getLogger(TimeRequestServer.class.getName());

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new GreeterImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    TimeRequestServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final TimeRequestServer server = new TimeRequestServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class GreeterImpl extends TimeGrpc.TimeImplBase {

        @Override
        public void requestTime(TimeRequestItem req, StreamObserver<TimeItem> responseObserver) {
            ZoneId zone = ZoneId.of(req.getTimeZone());
            ZonedDateTime zdf = ZonedDateTime.now(zone);
            TimeItem response = TimeItem.newBuilder().setTime(zdf.toString()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }

    }
}
