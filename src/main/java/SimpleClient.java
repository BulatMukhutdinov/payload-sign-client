import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import jp.co.soramitsu.payloadsignservice.CreateAccountRequest;
import jp.co.soramitsu.payloadsignservice.PayloadSignServiceGrpc;
import jp.co.soramitsu.payloadsignservice.Status;
import jp.co.soramitsu.payloadsignservice.StatusRequest;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class SimpleClient {
    private final ManagedChannel channel;
    private final PayloadSignServiceGrpc.PayloadSignServiceBlockingStub blockingStub;

    private static final byte[] CLIENT_PUBLIC_KEY = hexStringToByteArray("4f9fd344815c314116edc0b06c6af2cb71254d87ec5ddc50dd4b0c7545eb6add");

    public SimpleClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    SimpleClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PayloadSignServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void greet() {
        ByteString hash;

        CreateAccountRequest request = CreateAccountRequest.newBuilder()
                .setAccountName("bulat")
                .setDomainId("kh")
                .setMainPubkey(ByteString.copyFrom(CLIENT_PUBLIC_KEY)).build();
        try {
            hash = blockingStub.createAccount(request).getHash();
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Status status;
                    StatusRequest statusRequest = StatusRequest.newBuilder()
                            .setHash(hash)
                            .build();
                    status = blockingStub.status(statusRequest).getStatus();
                    try {
                        shutdown();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, 5 * 1000);

        } catch (StatusRuntimeException e) {
            return;
        }
    }

    public static void main(String[] args) throws Exception {
        SimpleClient client = new SimpleClient("localhost", 50051);
        client.greet();
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
