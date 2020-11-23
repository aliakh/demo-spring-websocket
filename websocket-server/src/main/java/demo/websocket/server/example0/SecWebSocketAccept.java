package demo.websocket.server.example0;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecWebSocketAccept {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        String secWebSocketKey = "7c0RT+Z1px24ypyYfnPNbw==";
        String secWebSocketAccept = Base64
                .getEncoder()
                .encodeToString(
                        MessageDigest
                                .getInstance("SHA-1")
                                .digest((secWebSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                        .getBytes(StandardCharsets.UTF_8)
                                )
                );
        System.out.println(secWebSocketAccept); // O1a/o0MeFzoDgn+kCKR91UkYDO4=
    }
}
