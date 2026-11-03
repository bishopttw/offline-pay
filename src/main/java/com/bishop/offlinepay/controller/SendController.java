package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.dto.SendRequest;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.service.SendService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class SendController {

    private final SendService sendService;

    public SendController (SendService sendService) {
        this.sendService = sendService;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @GetMapping("/lookup")
    public ResponseEntity<Map<String, String>> lookup(@RequestParam String email, HttpSession session) {
        String name = sendService.lookupName(currentId(session), email);
        if (name == null) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No OfflinePay account with that email."));
        }
        return ResponseEntity.ok(Map.of("name", name));
    }

    @PostMapping("/send")
    public Map<String, Object> send(@Valid @RequestBody SendRequest request, HttpSession session){
        Transaction tx = sendService.sendOnline(currentId(session), request);
        return Map.of("status", tx.getStatus().name(), "fee", tx.getFee());
    }

    private Long currentId(HttpSession session) {
        Long id = (Long) session.getAttribute("accountId");
        if (id == null) {
            throw new RuntimeException("You must be logged in");
        }
        return id;
    }
}
