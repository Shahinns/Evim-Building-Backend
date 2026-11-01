package org.example.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthTokenResponse {

    private String token;
    private String email;
    private String fullName;
    private String role;
    private Double balance;
    private Long id;
    private String phone;
    private Integer listingLimit;
    private String packageType;
    /** Qeydiyyat üçün məlumat mesajı */
    private String message;

    public AuthTokenResponse() {
    }

    public static AuthTokenResponse login(String token, String email, String fullName, String role, Double balance) {
        AuthTokenResponse r = new AuthTokenResponse();
        r.token = token;
        r.email = email;
        r.fullName = fullName != null ? fullName : "";
        r.role = role;
        r.balance = balance != null ? balance : 0.0;
        return r;
    }

    public static AuthTokenResponse fromUser(String token, org.example.entity.User user) {
        AuthTokenResponse r = login(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getBalance()
        );
        r.id = user.getId();
        r.phone = user.getPhone();
        r.listingLimit = user.getListingLimit() != null ? user.getListingLimit() : 10;
        r.packageType = user.getPackageType();
        return r;
    }

    public static AuthTokenResponse register(String token, String email, String fullName, String role, Double balance) {
        AuthTokenResponse r = login(token, email, fullName, role, balance);
        r.message = "Qeydiyyat uğurlu oldu";
        return r;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getListingLimit() { return listingLimit; }
    public void setListingLimit(Integer listingLimit) { this.listingLimit = listingLimit; }

    public String getPackageType() { return packageType; }
    public void setPackageType(String packageType) { this.packageType = packageType; }
}
