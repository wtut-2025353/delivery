package com.wilsontut.delivery.model;

import com.wilsontut.delivery.enums.CategoriaComercio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComercioRequest {

    @NotBlank(message = "El nombre del comercio es obligatorio")
    private String nombre;

    @NotNull(message = "La categoría es obligatoria (RESTAURANTE, SUPERMERCADO, FARMACIA)")
    private CategoriaComercio categoria;

    @NotBlank(message = "La dirección es obligatoria")
    private String direccion;

    @NotNull(message = "El estado abierto/cerrado es obligatorio")
    private Boolean abierto;
}
