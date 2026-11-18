package com.wilsontut.delivery.model;

import com.wilsontut.delivery.enums.EstadoPedido;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoResponse {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private String clienteEmail;
    private Long repartidorId;
    private String repartidorNombre;
    private LocalDateTime fechaPedido;
    private BigDecimal costoEnvio;
    private BigDecimal montoTotal;
    private EstadoPedido estado;
    private List<DetallePedidoResponse> detalles;
}
