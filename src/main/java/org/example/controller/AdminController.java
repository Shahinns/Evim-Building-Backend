package org.example.controller;

import org.example.entity.User;
import org.example.Interface.UserRepository;
import org.example.service.AgencyPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) return false;
        String email = (String) authentication.getPrincipal();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        return userOpt.isPresent() && "ADMIN".equalsIgnoreCase(userOpt.get().getRole());
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("message", "İcazə yoxdur"));

        String finalQuery = query.trim();
        if (finalQuery.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Axtarış ən az 2 simvol olmalıdır"));
        }

        List<User> users = userRepository.searchByEmailOrPhone(finalQuery);

        for (User u : users) {
            u.setPassword(null);
            u.setVerificationCode(null);
            u.setVerificationCodeExpiresAt(null);
        }

        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates, Authentication auth) {
        if (!isAdmin(auth)) return ResponseEntity.status(403).body(Map.of("message", "İcazə yoxdur"));

        Optional<User> targetOpt = userRepository.findById(id);
        if (targetOpt.isEmpty()) return ResponseEntity.notFound().build();

        User user = targetOpt.get();

        if (updates.containsKey("balance")) {
            try {
                user.setBalance(Double.parseDouble(updates.get("balance").toString()));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("role")) {
            String role = (String) updates.get("role");
            if (role != null && List.of("USER", "AGENCY", "ADMIN").contains(role.toUpperCase())) {
                user.setRole(role.toUpperCase());
            }
        }
        if (updates.containsKey("listingLimit")) {
            try {
                user.setListingLimit(Integer.parseInt(updates.get("listingLimit").toString()));
            } catch (Exception ignored) {}
        }
        if (updates.containsKey("packageType")) {
            String pkg = (String) updates.get("packageType");
            user.setPackageType(pkg);
            if (pkg != null && AgencyPackageService.PACKAGES.containsKey(pkg.toUpperCase().trim())) {
                AgencyPackageService.apply(user, AgencyPackageService.require(pkg));
            }
        }
        if (updates.containsKey("companyName")) {
            user.setCompanyName((String) updates.get("companyName"));
        }

        userRepository.save(user);
        user.setPassword(null);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        return ResponseEntity.ok(user);
    }
}
