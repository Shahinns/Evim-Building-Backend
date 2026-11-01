package org.example.security;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern STRONG_ENOUGH = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d).{8,}$"
    );

    private PasswordValidator() {}

    public static boolean isValid(String password) {
        return password != null && STRONG_ENOUGH.matcher(password).matches();
    }

    public static String requirementMessage() {
        return "Şifrə ən az 8 simvol, hərf və rəqəm olmalıdır.";
    }
}
