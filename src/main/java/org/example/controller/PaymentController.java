package org.example.controller;

import org.example.Interface.UserRepository;
import org.example.entity.User;
import org.example.service.AgencyPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private static final Set<Double> TOPUP_AMOUNTS = Set.of(10.0, 20.0, 50.0, 70.0, 100.0, 200.0);

    @Autowired
    private UserRepository userRepository;

    private Optional<User> currentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase((String) authentication.getPrincipal());
    }

    @GetMapping("/packages")
    public ResponseEntity<?> packages() {
        List<Map<String, Object>> list = AgencyPackageService.PACKAGES.values().stream()
                .map(p -> Map.<String, Object>of(
                        "code", p.code(),
                        "name", p.name(),
                        "price", p.price(),
                        "listingLimit", p.listingLimit()
                ))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/topup")
    public ResponseEntity<?> topupBalance(@RequestBody Map<String, Object> body, Authentication authentication) {
        Optional<User> userOpt = currentUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }

        double amount;
        try {
            amount = Double.parseDouble(String.valueOf(body.get("amount")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Məbləğ düzgün deyil"));
        }
        if (!TOPUP_AMOUNTS.contains(amount)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Yalnız 10, 20, 50, 70, 100 və ya 200 AZN seçin"));
        }

        User user = userOpt.get();
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Onlayn ödəniş sistemi tezliklə aktivləşdiriləcək. Balans artırmaq üçün zəhmət olmasa adminlə əlaqə saxlayın."));
        }
        double current = user.getBalance() != null ? user.getBalance() : 0.0;
        user.setBalance(current + amount);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", amount + " AZN balansınıza əlavə olundu",
                "balance", user.getBalance(),
                "listingLimit", user.getListingLimit() != null ? user.getListingLimit() : 10,
                "role", user.getRole(),
                "packageType", user.getPackageType() != null ? user.getPackageType() : ""
        ));
    }

    @PostMapping("/buy-package")
    public ResponseEntity<?> buyPackage(@RequestBody Map<String, String> body, Authentication authentication) {
        Optional<User> userOpt = currentUser(authentication);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }

        User user = userOpt.get();
        if (!"AGENCY".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Paket almaq üçün əvvəlcə admin agentlik hesabını təsdiqləməlidir."
            ));
        }

        AgencyPackageService.PackageOffer offer = AgencyPackageService.require(body.get("code"));
        double balance = user.getBalance() != null ? user.getBalance() : 0.0;
        if (balance < offer.price()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Balansınız kifayət etmir. " + offer.price() + " AZN lazımdır."
            ));
        }

        user.setBalance(balance - offer.price());
        AgencyPackageService.apply(user, offer);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", offer.name() + " paketi aktivləşdi. Elan limiti: " + offer.listingLimit(),
                "balance", user.getBalance(),
                "listingLimit", user.getListingLimit(),
                "role", user.getRole(),
                "packageType", user.getPackageType()
        ));
    }
}
