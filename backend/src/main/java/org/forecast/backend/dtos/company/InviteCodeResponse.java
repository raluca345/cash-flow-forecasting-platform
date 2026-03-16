package org.forecast.backend.dtos.company;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class InviteCodeResponse {
    private String inviteCode;
    private Instant expiresAt;
}

