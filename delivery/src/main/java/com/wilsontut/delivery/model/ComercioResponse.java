package com.wilsontut.delivery.model;

import com.wilsontut.delivery.enums.CategoriaComercio;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComercioResponse {

    private Long id;
    private String nombre;
    private CategoriaComercio categoria;
    private String direccion;
    private Boolean abierto;
}
