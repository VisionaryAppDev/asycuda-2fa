package com.example.demo;


import com.corundumstudio.socketio.SocketIOServer;
import com.example.demo.model.Otp;
import com.example.demo.model.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(path = "/2fa")
public class QrController {
    public final static Map<String, Token> tokens = new HashMap<>();
    public final static Map<String, Otp> otps = new HashMap<>();


    @Autowired
    private SocketIOServer server;


    @GetMapping("/qr/{userId}/{token}")
    public ResponseEntity<Void> getQr(@PathVariable("userId") String userId, @PathVariable("token") String qrToken) {
        Token token = tokens.get(userId);

        if(token != null && token.getToken().equals(qrToken)) {
            UUID clientId = token.getClientId();
            server.getClient(clientId).sendEvent("on_2fa_qr_successfully_scanned", Map.of("status", "done"));
            QrController.tokens.remove(userId);

            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/otp/{userId}/{token}")
    public ResponseEntity<Object> generateOtp(@PathVariable("userId") String userId, @PathVariable("token") String token) {
        /// logic
        Otp otp = new Otp();
        otp.setCreatedAt(Instant.now());
        otp.setUserId(userId);
        otp.setOtp((int) (Math.random() * 999999));
        otps.put(userId, otp);

        ///
        System.out.print(LocalDateTime.now());
        System.out.println(" - OTP:: "+ otp.getOtp());

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/otp/verify/{userId}/{otp}")
    public ResponseEntity<Object> validateOtp(@PathVariable("userId") String userId, @PathVariable("otp") int otp) {
        if(otps.get(userId) != null && otps.get(userId).getOtp() == otp) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @PostMapping("/{userId}/{fingerprint}")
    public ResponseEntity<Object> validateFingerprint(@PathVariable("userId") String userId, @PathVariable("fingerprint") byte[] fingerprint) {

        if (userId == null || fingerprint == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        /// logic

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
