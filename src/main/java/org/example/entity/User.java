package org.example.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Həmişa DB-də NOT NULL saxlanılır. Google hesabları üçün təsadüfi hash (şifrə ilə giriş olmur).
     */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    private String fullName;
    
    @Column(unique = true)
    private String phone;
    private String role = "USER"; // USER, ADMIN, AGENCY

    // ── Agentlik (Biznes Hesabı) məlumatları ─────────────────────────────────
    private String companyName;
    private String companyLogo;
    private String packageType; // Məs: "STANDART", "PRO"
    private Integer listingLimit = 10; // Adi istifadəçi üçün 10, Standart üçün 20, Pro üçün 9999

    /** LOCAL və ya GOOGLE */
    @Column(length = 32)
    private String authProvider = "LOCAL";

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean enabled = false;
    @JsonIgnore
    private String verificationCode;

    /** OTP etibarlılıq müddəti */
    @JsonIgnore
    private java.time.LocalDateTime verificationCodeExpiresAt;

    @Column(nullable = false, columnDefinition = "DOUBLE PRECISION DEFAULT 0.0")
    private Double balance = 0.0;

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance != null ? balance : 0.0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public java.time.LocalDateTime getVerificationCodeExpiresAt() {
        return verificationCodeExpiresAt;
    }

    public void setVerificationCodeExpiresAt(java.time.LocalDateTime verificationCodeExpiresAt) {
        this.verificationCodeExpiresAt = verificationCodeExpiresAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyLogo() {
        return companyLogo;
    }

    public void setCompanyLogo(String companyLogo) {
        this.companyLogo = companyLogo;
    }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType(String packageType) {
        this.packageType = packageType;
    }

    public Integer getListingLimit() {
        return listingLimit;
    }

    public void setListingLimit(Integer listingLimit) {
        this.listingLimit = listingLimit;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public User(Long id, String email, String password, String fullName, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
    public User(){}
}
