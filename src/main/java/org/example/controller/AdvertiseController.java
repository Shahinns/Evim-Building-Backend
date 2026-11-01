package org.example.controller;

import org.example.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AdvertiseController {

    @Autowired
    private EmailService emailService;

    @PostMapping("/advertise")
    public ResponseEntity<?> advertise(@RequestBody Map<String, String> body) {
        String name = safe(body.get("name"));
        String phone = safe(body.get("phone"));
        String email = safe(body.get("email"));
        String company = safe(body.get("company"));
        String plan = safe(body.get("plan"));
        String message = safe(body.get("message"));

        if (name.isBlank() || phone.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ad, telefon və email məcburidir"));
        }

        emailService.sendAdvertiseInquiry(name, phone, email, company, plan, message);
        return ResponseEntity.ok(Map.of("message", "Müraciətiniz qəbul olundu. Tezliklə sizinlə əlaqə saxlanılacaq."));
    }

    @PostMapping("/agency/apply")
    public ResponseEntity<?> applyAgency(@RequestBody Map<String, String> body) {
        String name = safe(body.get("name"));
        String phone = safe(body.get("phone"));
        String email = safe(body.get("email"));
        String company = safe(body.get("company"));
        String note = safe(body.get("message"));

        if (name.isBlank() || phone.isBlank() || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ad, telefon və email məcburidir"));
        }

        emailService.sendAgencyApplication(name, phone, email, company, note);
        return ResponseEntity.ok(Map.of(
                "message", "Agentlik müraciətiniz göndərildi. Admin təsdiqindən sonra hesabınız aktiv olacaq."
        ));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
