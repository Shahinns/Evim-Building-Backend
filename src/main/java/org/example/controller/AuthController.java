package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.DTO.AuthTokenResponse;
import org.example.DTO.GoogleLoginRequest;
import org.example.DTO.LoginRequest;
import org.example.DTO.RegisterRequest;
import org.example.Interface.PasswordResetTokenRepository;
import org.example.Interface.UserRepository;
import org.example.entity.PasswordResetToken;
import org.example.entity.User;
import org.example.security.AuthCookieService;
import org.example.security.AuthRateLimitService;
import org.example.security.JwtUtil;
import org.example.security.PasswordValidator;
import org.example.security.VerificationCodeUtil;
import org.example.service.EmailService;
import org.example.service.GoogleIdTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final int OTP_VALID_MINUTES = 10;

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private GoogleIdTokenService googleIdTokenService;
    @Autowired private EmailService emailService;
    @Autowired private PasswordResetTokenRepository resetTokenRepository;
    @Autowired private AuthCookieService authCookieService;
    @Autowired private AuthRateLimitService authRateLimitService;

    private boolean isMobileClient(HttpServletRequest request) {
        return "mobile".equalsIgnoreCase(request.getHeader("X-Client-Type"));
    }

    private ResponseEntity<?> authSuccess(User user, HttpServletRequest request, HttpServletResponse response) {
        String token = jwtUtil.generateToken(user.getEmail());
        authCookieService.setAuthCookie(response, token);

        return ResponseEntity.ok(AuthTokenResponse.fromUser(token, user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Giriş tələb olunur"));
        }
        String email = (String) authentication.getPrincipal();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "İstifadəçi tapılmadı"));
        }
        User user = userOpt.get();
        return ResponseEntity.ok(AuthTokenResponse.fromUser(null, user));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        authCookieService.clearAuthCookie(response);
        return ResponseEntity.ok(Map.of("message", "Çıxış edildi"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Düzgün email formatı daxil edin."));
        }
        if (!PasswordValidator.isValid(req.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", PasswordValidator.requirementMessage()));
        }
        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User u = existing.get();
            if ("GOOGLE".equalsIgnoreCase(u.getAuthProvider())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Bu email Google hesabı ilə bağlıdır — Google ilə daxil olun."));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Bu email artıq mövcuddur"));
        }
        User user = new User();
        user.setEmail(email);
        user.setFullName(req.getFullName());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setAuthProvider("LOCAL");
        assignVerificationCode(user);
        user.setEnabled(false);
        userRepository.save(user);
        emailService.sendVerificationCode(user.getEmail(), user.getVerificationCode());
        return ResponseEntity.ok(Map.of("message", "Təsdiq kodu emailinizə göndərildi."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(req.getEmail().trim());
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Email və ya şifrə yanlışdır"));
        }
        User user = optionalUser.get();
        if ("GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            return ResponseEntity.status(401).body(Map.of("message", "Bu hesab üçün Google ilə daxil olun"));
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Email və ya şifrə yanlışdır"));
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("message", "Email və ya şifrə yanlışdır"));
        }
        if (!user.isEnabled()) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Hesabınız təsdiqlənməyib. Zəhmət olmasa emailinizə gələn kodu daxil edin.",
                    "requiresVerification", true,
                    "email", user.getEmail()
            ));
        }
        return authSuccess(user, request, response);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(
            @RequestBody GoogleLoginRequest req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!googleIdTokenService.isConfigured()) {
            return ResponseEntity.status(503).body(Map.of(
                    "message", "Google girişi serverdə qurulmayıb (GOOGLE_CLIENT_ID)."));
        }
        Optional<GoogleIdTokenService.GoogleProfile> profileOpt =
                googleIdTokenService.verifyAndExtract(req.getCredential());
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Google təsdiqi uğursuz oldu"));
        }
        GoogleIdTokenService.GoogleProfile profile = profileOpt.get();
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(profile.email());
        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.setEnabled(true);
            boolean changed = false;
            if (user.getFullName() == null || user.getFullName().isBlank()) {
                user.setFullName(profile.fullName());
                changed = true;
            }
            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setAuthProvider("GOOGLE");
                changed = true;
            }
            if (changed) userRepository.save(user);
        } else {
            user = new User();
            user.setEmail(profile.email());
            user.setFullName(profile.fullName() != null && !profile.fullName().isBlank()
                    ? profile.fullName()
                    : profile.email().split("@")[0]);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setAuthProvider("GOOGLE");
            user.setEnabled(true);
            userRepository.save(user);
        }
        return authSuccess(user, request, response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim().toLowerCase();
        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email daxil edin"));
        }
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if ("GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Bu hesab Google ilə bağlıdır. Zəhmət olmasa Google ilə daxil olun."));
            }
            resetTokenRepository.deleteAllByEmail(email);
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken(
                    token, email, LocalDateTime.now().plusMinutes(30));
            resetTokenRepository.save(resetToken);
            try {
                emailService.sendPasswordResetEmail(email, token);
            } catch (Exception e) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("message", "Email göndərilmədi. Sonra yenidən cəhd edin."));
            }
        }
        return ResponseEntity.ok(Map.of("message", "Əgər bu email mövcuddursa, sıfırlama linki göndərildi"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token    = body.getOrDefault("token", "").trim();
        String password = body.getOrDefault("password", "").trim();
        if (token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token yoxdur"));
        }
        if (!PasswordValidator.isValid(password)) {
            return ResponseEntity.badRequest().body(Map.of("message", PasswordValidator.requirementMessage()));
        }
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Token tapılmadı və ya etibarsızdır"));
        }
        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.isUsed()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu token artıq istifadə edilib"));
        }
        if (resetToken.isExpired()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tokenin müddəti bitib. Yenidən sorğu göndərin."));
        }
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(resetToken.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "İstifadəçi tapılmadı"));
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
        return ResponseEntity.ok(Map.of("message", "Şifrə uğurla dəyişdirildi"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @RequestBody Map<String, String> req,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String email = req.get("email");
        String code = req.get("code");
        if (email == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email və kod daxil edilməlidir"));
        }
        email = email.trim().toLowerCase();

        if (authRateLimitService.isVerifyLocked(email)) {
            return ResponseEntity.status(429).body(Map.of(
                    "message", "Çox sayda uğursuz cəhd. 15 dəqiqə sonra yenidən cəhd edin."));
        }

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "İstifadəçi tapılmadı"));
        }
        User user = optionalUser.get();
        String savedCode = user.getVerificationCode();

        if (savedCode == null || user.getVerificationCodeExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getVerificationCodeExpiresAt())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Kodun müddəti bitib. Yeni kod tələb edin."));
        }

        if (savedCode.equals(code.trim())) {
            user.setEnabled(true);
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
            authRateLimitService.clearVerifyAttempts(email);
            return authSuccess(user, request, response);
        }

        authRateLimitService.recordFailedVerify(email);
        return ResponseEntity.badRequest().body(Map.of("message", "Daxil etdiyiniz təsdiq kodu yanlışdır"));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<?> resendCode(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email daxil edin"));
        }
        email = email.trim().toLowerCase();

        if (!authRateLimitService.allowResend(email)) {
            return ResponseEntity.status(429).body(Map.of(
                    "message", "Zəhmət olmasa 1 dəqiqə gözləyin və yenidən cəhd edin."));
        }

        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "Əgər bu email mövcuddursa, yeni kod göndərildi"));
        }
        User user = optionalUser.get();
        assignVerificationCode(user);
        userRepository.save(user);
        emailService.sendVerificationCode(user.getEmail(), user.getVerificationCode());
        return ResponseEntity.ok(Map.of("message", "Yeni təsdiq kodu göndərildi"));
    }

    @PostMapping("/quick-register")
    public ResponseEntity<?> quickRegister(@RequestBody Map<String, String> req) {
        String email    = req.get("email");
        String phone    = req.get("phone");
        String fullName = req.get("fullName");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email məcburidir"));
        }

        email = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Düzgün email formatı daxil edin."));
        }

        if (phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Telefon nömrəsi məcburidir"));
        }
        String normalizedPhone = phone.trim();

        Optional<User> byPhone = userRepository.findByPhone(normalizedPhone);
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (byPhone.isPresent() && (optionalUser.isEmpty() || !byPhone.get().getId().equals(optionalUser.get().getId()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bu telefon nömrəsi artıq istifadə olunur"));
        }

        User user;
        if (optionalUser.isPresent()) {
            user = optionalUser.get();
            user.setPhone(normalizedPhone);
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
            }
        } else {
            user = new User();
            user.setEmail(email);
            user.setPhone(normalizedPhone);
            user.setFullName(fullName != null ? fullName : "");
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setAuthProvider("LOCAL");
            user.setEnabled(false);
            user.setListingLimit(10);
            user.setRole("USER");
        }

        if (!authRateLimitService.allowResend(email)) {
            return ResponseEntity.status(429).body(Map.of(
                    "message", "Zəhmət olmasa 1 dəqiqə gözləyin və yenidən cəhd edin."));
        }

        assignVerificationCode(user);
        userRepository.save(user);
        emailService.sendVerificationCode(user.getEmail(), user.getVerificationCode());
        return ResponseEntity.ok(Map.of("message", "Təsdiq kodu emailinizə göndərildi. Telefon nömrəniz hesabınıza bağlanır."));
    }

    private void assignVerificationCode(User user) {
        user.setVerificationCode(VerificationCodeUtil.generateSixDigitCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(OTP_VALID_MINUTES));
    }
}