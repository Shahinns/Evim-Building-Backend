package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.example.Interface.UserRepository;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthCookieService authCookieService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AuthCookieService authCookieService, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.authCookieService = authCookieService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = authCookieService.extractToken(request);
            if (token == null || !jwtUtil.isValid(token)) {
                String header = request.getHeader("Authorization");
                if (header != null && header.startsWith("Bearer ")) {
                    String bearer = header.substring(7).trim();
                    if (jwtUtil.isValid(bearer)) {
                        token = bearer;
                    }
                }
            }

            if (token != null && !token.isEmpty() && jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                String userRole = "USER";
                try {
                    userRole = userRepository.findByEmailIgnoreCase(email)
                            .map(u -> u.getRole() != null ? u.getRole().trim().toUpperCase() : "USER")
                            .orElse("USER");
                } catch (Exception e) {
                    userRole = "USER";
                }

                var authorities = "USER".equals(userRole)
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        : List.of(new SimpleGrantedAuthority("ROLE_" + userRole), new SimpleGrantedAuthority("ROLE_USER"));

                var auth = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ignored) {
            // Token keçərsizdirsə SecurityContext doldurulmur
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        // 🔴 ƏSAS DÜZƏLİŞ: Şəkil sorğularında JWT filtrini tam söndürürük
        if (path.startsWith("/uploads/") || path.startsWith("/api/uploads/")) {
            return true;
        }

        if (!path.startsWith("/api/auth")) {
            return false;
        }
        return !path.equals("/api/auth/me") && !path.equals("/api/auth/logout");
    }
}