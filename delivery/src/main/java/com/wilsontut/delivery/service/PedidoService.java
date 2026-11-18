package com.wilsontut.delivery.service;

import com.wilsontut.delivery.entity.DetallePedido;
import com.wilsontut.delivery.entity.Pedido;
import com.wilsontut.delivery.entity.Producto;
import com.wilsontut.delivery.entity.Usuario;
import com.wilsontut.delivery.enums.EstadoPedido;
import com.wilsontut.delivery.enums.Rol;
import com.wilsontut.delivery.exception.InsufficientStockException;
import com.wilsontut.delivery.exception.InvalidStatusException;
import com.wilsontut.delivery.exception.ResourceNotFoundException;
import com.wilsontut.delivery.model.*;
import com.wilsontut.delivery.repository.PedidoRepository;
import com.wilsontut.delivery.repository.ProductoRepository;
import com.wilsontut.delivery.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final BigDecimal COSTO_ENVIO_FIJO = new BigDecimal("20.00");

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(rollbackFor = Exception.class)
    public PedidoResponse crearPedido(CrearPedidoRequest request, String clienteEmail) {
        Usuario cliente = usuarioRepository.findByEmail(clienteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + clienteEmail));

        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .fechaPedido(LocalDateTime.now())
                .costoEnvio(COSTO_ENVIO_FIJO)
                .estado(EstadoPedido.PENDIENTE)
                .build();

        BigDecimal subtotalAcumulado = BigDecimal.ZERO;

        for (DetallePedidoRequest item : request.getItems()) {
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + item.getProductoId()));

            if (Boolean.FALSE.equals(producto.getDisponible())) {
                throw new InsufficientStockException("El producto " + producto.getNombre() + " no está disponible actualmente");
            }

            if (producto.getStock() < item.getCantidad()) {
                throw new InsufficientStockException("Stock insuficiente para el producto: " + producto.getNombre() +
                        ". Solicitado: " + item.getCantidad() + ", Disponible: " + producto.getStock());
            }

            // Descontar inventario
            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            // Calcular subtotal del ítem
            BigDecimal precioUnitario = producto.getPrecio();
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));
            subtotalAcumulado = subtotalAcumulado.add(subtotalItem);

            DetallePedido detalle = DetallePedido.builder()
                    .producto(producto)
                    .cantidad(item.getCantidad())
                    .precioUnitario(precioUnitario)
                    .subtotal(subtotalItem)
                    .build();

            pedido.addDetalle(detalle);
        }

        // Total = Suma de subtotales + Costo de envío
        BigDecimal montoTotal = subtotalAcumulado.add(COSTO_ENVIO_FIJO);
        pedido.setMontoTotal(montoTotal);

        Pedido guardado = pedidoRepository.save(pedido);
        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarMisPedidos(String clienteEmail) {
        Usuario cliente = usuarioRepository.findByEmail(clienteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con email: " + clienteEmail));

        return pedidoRepository.findByClienteOrderByFechaPedidoDesc(cliente).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPedidosDisponibles() {
        // Pedidos disponibles para entrega o asignación
        List<EstadoPedido> estadosDisponibles = Arrays.asList(
                EstadoPedido.PENDIENTE,
                EstadoPedido.EN_PREPARACION,
                EstadoPedido.EN_CAMINO
        );

        return pedidoRepository.findByEstadoInOrderByFechaPedidoDesc(estadosDisponibles).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public PedidoResponse cambiarEstadoPedido(Long pedidoId, EstadoPedido nuevoEstado, String userEmail) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + userEmail));

        validarTransicionEstado(pedido.getEstado(), nuevoEstado);

        // Si es repartidor y el pedido aún no tiene repartidor asignado, asignarlo
        if (usuario.getRol() == Rol.REPARTIDOR && pedido.getRepartidor() == null) {
            pedido.setRepartidor(usuario);
        }

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);
        return mapToResponse(actualizado);
    }

    @Transactional(rollbackFor = Exception.class)
    public PedidoResponse cancelarPedido(Long pedidoId, String userEmail) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con ID: " + pedidoId));

        Usuario usuario = usuarioRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + userEmail));

        // Validación de permisos de cancelación
        if (usuario.getRol() == Rol.CLIENTE && !pedido.getCliente().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tienes permiso para cancelar este pedido");
        }

        // Solo se pueden cancelar pedidos en estado PENDIENTE
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new InvalidStatusException("Solo se pueden cancelar pedidos en estado PENDIENTE. Estado actual: " + pedido.getEstado());
        }

        pedido.setEstado(EstadoPedido.CANCELADO);

        // Restaurar stock de cada producto del pedido
        for (DetallePedido detalle : pedido.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
        }

        Pedido cancelado = pedidoRepository.save(pedido);
        return mapToResponse(cancelado);
    }

    private void validarTransicionEstado(EstadoPedido estadoActual, EstadoPedido nuevoEstado) {
        if (estadoActual == nuevoEstado) {
            return;
        }

        boolean transicionValida = switch (estadoActual) {
            case PENDIENTE -> nuevoEstado == EstadoPedido.EN_PREPARACION || nuevoEstado == EstadoPedido.CANCELADO;
            case EN_PREPARACION -> nuevoEstado == EstadoPedido.EN_CAMINO;
            case EN_CAMINO -> nuevoEstado == EstadoPedido.ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };

        if (!transicionValida) {
            throw new InvalidStatusException("Transición no válida de " + estadoActual + " a " + nuevoEstado);
        }
    }

    public PedidoResponse mapToResponse(Pedido pedido) {
        List<DetallePedidoResponse> detalles = pedido.getDetalles().stream()
                .map(d -> DetallePedidoResponse.builder()
                        .id(d.getId())
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .subtotal(d.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return PedidoResponse.builder()
                .id(pedido.getId())
                .clienteId(pedido.getCliente() != null ? pedido.getCliente().getId() : null)
                .clienteNombre(pedido.getCliente() != null ? pedido.getCliente().getNombre() : null)
                .clienteEmail(pedido.getCliente() != null ? pedido.getCliente().getEmail() : null)
                .repartidorId(pedido.getRepartidor() != null ? pedido.getRepartidor().getId() : null)
                .repartidorNombre(pedido.getRepartidor() != null ? pedido.getRepartidor().getNombre() : null)
                .fechaPedido(pedido.getFechaPedido())
                .costoEnvio(pedido.getCostoEnvio())
                .montoTotal(pedido.getMontoTotal())
                .estado(pedido.getEstado())
                .detalles(detalles)
                .build();
    }
}
