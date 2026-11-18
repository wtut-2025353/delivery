package com.wilsontut.delivery.model;

import com.wilsontut.delivery.enums.Rol;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private String tipo;
    private String email;
    private Rol rol;
}
