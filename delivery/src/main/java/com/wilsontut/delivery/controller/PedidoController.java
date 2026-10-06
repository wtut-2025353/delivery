package com.wilsontut.delivery.controller;

import com.wilsontut.delivery.model.CambiarEstadoPedidoRequest;
import com.wilsontut.delivery.model.CrearPedidoRequest;
import com.wilsontut.delivery.model.PedidoResponse;
import com.wilsontut.delivery.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<PedidoResponse> crearPedido(
            @Valid @RequestBody CrearPedidoRequest request,
            Authentication authentication) {
        String clienteEmail = authentication.getName();
        PedidoResponse response = pedidoService.crearPedido(request, clienteEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<PedidoResponse>> listarMisPedidos(Authentication authentication) {
        String clienteEmail = authentication.getName();
        List<PedidoResponse> pedidos = pedidoService.listarMisPedidos(clienteEmail);
        return ResponseEntity.ok(pedidos);
    }

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN')")
    public ResponseEntity<List<PedidoResponse>> listarPedidosDisponibles() {
        List<PedidoResponse> pedidos = pedidoService.listarPedidosDisponibles();
        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('REPARTIDOR', 'ADMIN')")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoPedidoRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        PedidoResponse response = pedidoService.cambiarEstadoPedido(id, request.getEstado(), userEmail);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<PedidoResponse> cancelarPedido(
            @PathVariable Long id,
            Authentication authentication) {
        String userEmail = authentication.getName();
        PedidoResponse response = pedidoService.cancelarPedido(id, userEmail);
        return ResponseEntity.ok(response);
    }
}
