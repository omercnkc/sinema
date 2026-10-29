package com.sinema.auth.servis.dto;

import jakarta.validation.constraints.NotBlank;

/** POST /internal/v1/auth/service-token gövdesi (planlama §7.3). */
public record ServisTokeniIstegi(
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String audience) {

    @Override
    public String toString() {
        return "ServisTokeniIstegi[clientId=" + clientId + ", clientSecret=***, audience=" + audience + "]";
    }
}
