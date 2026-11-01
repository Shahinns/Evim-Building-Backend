package org.example.DTO;

public class GoogleLoginRequest {

    /** Google Identity Services JWT (credential) */
    private String credential;

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }
}
