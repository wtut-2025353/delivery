package com.wilsontut.delivery.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponse {

    private Long id;
    private Long comercioId;
    private String comercioNombre;
    private String nombre;
    private BigDecimal precio;
    private Integer stock;
    private Boolean disponible;
}
