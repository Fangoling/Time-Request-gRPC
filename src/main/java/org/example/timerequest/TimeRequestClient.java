package org.example.timerequest;

import com.example.time.protos.TimeGrpc;
import com.example.time.protos.TimeItem;
import com.example.time.protos.TimeRequestItem;
import io.grpc.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.time.Instant;

public class TimeRequestClient {

    private static final Logger logger = Logger.getLogger(TimeRequestClient.class.getName());

    private final TimeGrpc.TimeBlockingStub blockingStub;

    /** Construct client for accessing TimeRequest server using the existing channel. */
    public TimeRequestClient(Channel channel) {
        // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
        // shut it down.

        // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
        blockingStub = TimeGrpc.newBlockingStub(channel);
    }

    /** Request Time to server.*/
    public void request(int id){

        String timeZone = switch (id){
            case 2 -> "GMT";
            case 3 -> "ECT";
            default -> "UTC";
        };
        logger.info("Will try to get current Time of " + timeZone + "...");

        TimeRequestItem request = TimeRequestItem.newBuilder().setTimeZone(timeZone).build();
        TimeItem response;

        try{
            response = blockingStub.requestTime(request);
        } catch (StatusRuntimeException e){
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("The current Time is: " + response.getTime());
    }

    /**
     * Time-server. If provided, the first element of {@code args} is the requested
     * Timezone.
     * The second argument is the target server.
     */

    public static void main(String[] args) throws Exception{
        int id = 1;
        // Access a service running on a local machine on port 50051
        String target = "localhost:50051";
        // Allow passing in the request id and target strings as command line arguments
        if (args.length > 0){
            if ("--help".equals(args[0])){
                System.err.println("Usage: [timezone [target]] \n");
                System.err.println(" timezone   Id of the requested Timezone: 1 = UCT, 2 = GMT, 3 = CET. Defaults to " + id);
                System.err.println(" target     The server to connect to. Defaults to " + target);
                System.exit(1);
            }
            id = Integer.parseInt(args[0]);
        }
        if (args.length > 1){
            target = args[1];
        }

        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        //
        // For the example we use plaintext insecure credentials to avoid needing TLS certificates. To
        // use TLS, use TlsChannelCredentials instead.

        ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
        try{
            TimeRequestClient client = new TimeRequestClient(channel);
            client.request(id);
        } finally {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }


}
