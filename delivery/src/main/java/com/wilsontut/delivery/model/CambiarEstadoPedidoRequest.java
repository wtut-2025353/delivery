package com.wilsontut.delivery.model;

import com.wilsontut.delivery.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CambiarEstadoPedidoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoPedido estado;
}
