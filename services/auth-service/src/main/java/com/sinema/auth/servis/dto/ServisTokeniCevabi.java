package com.sinema.auth.servis.dto;

public record ServisTokeniCevabi(String accessToken, long expiresInSeconds) {

    @Override
    public String toString() {
        return "ServisTokeniCevabi[accessToken=***, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
