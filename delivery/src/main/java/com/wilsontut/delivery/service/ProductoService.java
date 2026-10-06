package com.wilsontut.delivery.service;

import com.wilsontut.delivery.entity.Comercio;
import com.wilsontut.delivery.entity.Producto;
import com.wilsontut.delivery.exception.ResourceNotFoundException;
import com.wilsontut.delivery.model.ProductoRequest;
import com.wilsontut.delivery.model.ProductoResponse;
import com.wilsontut.delivery.repository.ComercioRepository;
import com.wilsontut.delivery.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ComercioRepository comercioRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarProductosPorComercio(Long comercioId) {
        if (!comercioRepository.existsById(comercioId)) {
            throw new ResourceNotFoundException("Comercio no encontrado con ID: " + comercioId);
        }

        return productoRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductoResponse agregarProducto(Long comercioId, ProductoRequest request) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado con ID: " + comercioId));

        Producto producto = Producto.builder()
                .comercio(comercio)
                .nombre(request.getNombre())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .disponible(request.getDisponible())
                .build();

        Producto guardado = productoRepository.save(producto);
        return mapToResponse(guardado);
    }

    public ProductoResponse mapToResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .comercioId(producto.getComercio().getId())
                .comercioNombre(producto.getComercio().getNombre())
                .nombre(producto.getNombre())
                .precio(producto.getPrecio())
                .stock(producto.getStock())
                .disponible(producto.getDisponible())
                .build();
    }
}
