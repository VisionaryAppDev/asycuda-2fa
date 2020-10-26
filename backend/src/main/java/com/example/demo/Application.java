package com.example.demo;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.example.demo.model.Token;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(9092);
        config.setAllowCustomRequests(true);


        SocketIOServer server = new SocketIOServer(config);
        server.start();

        server.addEventListener("on_2fa_qr_clicked", Map.class, new DataListener<Map>() {
            @Override
            public void onData(final SocketIOClient client, Map data, final AckRequest ackRequest) {
                String userId = (String) data.get("userId");
                UUID clientId = client.getSessionId();
                Token token = new Token();
                token.setCreatedAt(Instant.now());
                token.setUserId(userId);
                token.setToken(UUID.randomUUID().toString());
                token.setClientId(clientId);


                Map<String, Object> qr = new HashMap<>();
                String url = "http://10.0.8.104:8080/2fa/qr/"+ token.getUserId() + "/" + token.getToken();
                qr.put("url", url);
                server.getClient(clientId).sendEvent("on_2fa_qr_clicked", qr);
                QrController.tokens.put(userId, token);

                System.out.println(url);
            }
        });

        return server;
    }
}
