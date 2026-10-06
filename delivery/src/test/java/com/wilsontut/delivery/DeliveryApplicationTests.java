package com.wilsontut.delivery;

import com.wilsontut.delivery.config.JwtTokenProvider;
import com.wilsontut.delivery.entity.Comercio;
import com.wilsontut.delivery.entity.Pedido;
import com.wilsontut.delivery.entity.Producto;
import com.wilsontut.delivery.entity.Usuario;
import com.wilsontut.delivery.enums.CategoriaComercio;
import com.wilsontut.delivery.enums.EstadoPedido;
import com.wilsontut.delivery.enums.Rol;
import com.wilsontut.delivery.exception.InsufficientStockException;
import com.wilsontut.delivery.exception.InvalidStatusException;
import com.wilsontut.delivery.model.CrearPedidoRequest;
import com.wilsontut.delivery.model.DetallePedidoRequest;
import com.wilsontut.delivery.model.PedidoResponse;
import com.wilsontut.delivery.repository.ComercioRepository;
import com.wilsontut.delivery.repository.PedidoRepository;
import com.wilsontut.delivery.repository.ProductoRepository;
import com.wilsontut.delivery.repository.UsuarioRepository;
import com.wilsontut.delivery.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationTests {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario clienteTest;
    private Usuario repartidorTest;
    private Comercio comercioTest;
    private Producto productoTest;

    @BeforeEach
    void setUp() {
        clienteTest = Usuario.builder()
                .id(1L)
                .nombre("Cliente Test")
                .email("cliente@test.com")
                .direccion("Zona 10")
                .telefono("12345678")
                .rol(Rol.CLIENTE)
                .build();

        repartidorTest = Usuario.builder()
                .id(2L)
                .nombre("Repartidor Test")
                .email("repartidor@test.com")
                .rol(Rol.REPARTIDOR)
                .build();

        comercioTest = Comercio.builder()
                .id(1L)
                .nombre("Restaurante Test")
                .categoria(CategoriaComercio.RESTAURANTE)
                .direccion("Zona 1")
                .abierto(true)
                .build();

        productoTest = Producto.builder()
                .id(10L)
                .nombre("Hamburguesa")
                .precio(new BigDecimal("50.00"))
                .stock(10)
                .disponible(true)
                .comercio(comercioTest)
                .build();
    }

    @Test
    @DisplayName("Debe crear un pedido calculando subtotales y agregando Q20.00 de envío")
    void testCrearPedidoCalculoCorrecto() {
        CrearPedidoRequest request = CrearPedidoRequest.builder()
                .items(Collections.singletonList(
                        DetallePedidoRequest.builder()
                                .productoId(10L)
                                .cantidad(2)
                                .build()
                ))
                .build();

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(clienteTest));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoTest));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        PedidoResponse response = pedidoService.crearPedido(request, "cliente@test.com");

        assertNotNull(response);
        assertEquals(new BigDecimal("120.00"), response.getMontoTotal()); // 2 * 50 = 100 + 20 envío = 120.00
        assertEquals(new BigDecimal("20.00"), response.getCostoEnvio());
        assertEquals(8, productoTest.getStock()); // Stock reducido de 10 a 8
        assertEquals(EstadoPedido.PENDIENTE, response.getEstado());
        verify(productoRepository, times(1)).save(productoTest);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe lanzar InsufficientStockException si el stock solicitado supera al disponible")
    void testCrearPedidoStockInsuficiente() {
        CrearPedidoRequest request = CrearPedidoRequest.builder()
                .items(Collections.singletonList(
                        DetallePedidoRequest.builder()
                                .productoId(10L)
                                .cantidad(20) // Disponible solo 10
                                .build()
                ))
                .build();

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(clienteTest));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(productoTest));

        assertThrows(InsufficientStockException.class, () -> {
            pedidoService.crearPedido(request, "cliente@test.com");
        });

        assertEquals(10, productoTest.getStock()); // Stock no modificado
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Debe permitir cancelar un pedido PENDIENTE y restaurar el stock al producto")
    void testCancelarPedidoYRestaurarStock() {
        Pedido pedido = Pedido.builder()
                .id(100L)
                .cliente(clienteTest)
                .fechaPedido(LocalDateTime.now())
                .costoEnvio(new BigDecimal("20.00"))
                .montoTotal(new BigDecimal("120.00"))
                .estado(EstadoPedido.PENDIENTE)
                .build();

        com.wilsontut.delivery.entity.DetallePedido detalle = com.wilsontut.delivery.entity.DetallePedido.builder()
                .id(1L)
                .pedido(pedido)
                .producto(productoTest)
                .cantidad(2)
                .precioUnitario(new BigDecimal("50.00"))
                .subtotal(new BigDecimal("100.00"))
                .build();

        pedido.addDetalle(detalle);
        productoTest.setStock(8); // Ya se habían descontado 2

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(clienteTest));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse response = pedidoService.cancelarPedido(100L, "cliente@test.com");

        assertEquals(EstadoPedido.CANCELADO, response.getEstado());
        assertEquals(10, productoTest.getStock()); // Stock restaurado de 8 a 10
        verify(productoRepository, times(1)).save(productoTest);
    }

    @Test
    @DisplayName("Debe lanzar InvalidStatusException si se intenta cancelar un pedido no PENDIENTE")
    void testCancelarPedidoEstadoInvalido() {
        Pedido pedido = Pedido.builder()
                .id(100L)
                .cliente(clienteTest)
                .estado(EstadoPedido.EN_CAMINO)
                .build();

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(clienteTest));

        assertThrows(InvalidStatusException.class, () -> {
            pedidoService.cancelarPedido(100L, "cliente@test.com");
        });
    }

    @Test
    @DisplayName("Debe validar transiciones de estado permitidas")
    void testTransicionEstadosValida() {
        Pedido pedido = Pedido.builder()
                .id(100L)
                .cliente(clienteTest)
                .estado(EstadoPedido.PENDIENTE)
                .build();

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("repartidor@test.com")).thenReturn(Optional.of(repartidorTest));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponse response = pedidoService.cambiarEstadoPedido(100L, EstadoPedido.EN_PREPARACION, "repartidor@test.com");
        assertEquals(EstadoPedido.EN_PREPARACION, response.getEstado());
        assertEquals(repartidorTest.getId(), response.getRepartidorId());
    }

    @Test
    @DisplayName("Debe rechazar transiciones de estado inválidas")
    void testTransicionEstadosInvalida() {
        Pedido pedido = Pedido.builder()
                .id(100L)
                .cliente(clienteTest)
                .estado(EstadoPedido.PENDIENTE)
                .build();

        when(pedidoRepository.findById(100L)).thenReturn(Optional.of(pedido));
        when(usuarioRepository.findByEmail("repartidor@test.com")).thenReturn(Optional.of(repartidorTest));

        assertThrows(InvalidStatusException.class, () -> {
            pedidoService.cambiarEstadoPedido(100L, EstadoPedido.ENTREGADO, "repartidor@test.com");
        });
    }

    @Test
    @DisplayName("Generación y validación de Token JWT")
    void testJwtTokenProvider() {
        JwtTokenProvider provider = new JwtTokenProvider();
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "jwtSecret", "DeliverySuperSecretKeyForJWTGeneration2025353KinalAppAuthSecretKeyHere!");
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "jwtExpirationInMs", 3600000L);

        String token = provider.generateToken("admin@delivery.com", Rol.ADMIN);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        assertEquals("admin@delivery.com", provider.getEmailFromToken(token));
        assertEquals("ADMIN", provider.getRoleFromToken(token));
    }
}
