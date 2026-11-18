package com.wilsontut.delivery.controller;

import com.wilsontut.delivery.enums.CategoriaComercio;
import com.wilsontut.delivery.model.ComercioRequest;
import com.wilsontut.delivery.model.ComercioResponse;
import com.wilsontut.delivery.model.ProductoRequest;
import com.wilsontut.delivery.model.ProductoResponse;
import com.wilsontut.delivery.service.ComercioService;
import com.wilsontut.delivery.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comercios")
@RequiredArgsConstructor
public class ComercioController {

    private final ComercioService comercioService;
    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<List<ComercioResponse>> listarComercios(
            @RequestParam(required = false) CategoriaComercio categoria) {
        List<ComercioResponse> comercios = comercioService.listarComercios(categoria);
        return ResponseEntity.ok(comercios);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComercioResponse> crearComercio(@Valid @RequestBody ComercioRequest request) {
        ComercioResponse response = comercioService.crearComercio(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/productos")
    public ResponseEntity<List<ProductoResponse>> listarProductos(@PathVariable Long id) {
        List<ProductoResponse> productos = productoService.listarProductosPorComercio(id);
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/{id}/productos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductoResponse> agregarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = productoService.agregarProducto(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
