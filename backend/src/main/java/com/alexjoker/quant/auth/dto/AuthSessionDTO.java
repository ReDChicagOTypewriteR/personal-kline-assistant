package com.alexjoker.quant.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionDTO {
    private String token;
    private String username;
    private String displayName;
}
