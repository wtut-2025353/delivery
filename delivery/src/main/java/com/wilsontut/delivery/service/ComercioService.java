package com.wilsontut.delivery.service;

import com.wilsontut.delivery.entity.Comercio;
import com.wilsontut.delivery.enums.CategoriaComercio;
import com.wilsontut.delivery.exception.ResourceNotFoundException;
import com.wilsontut.delivery.model.ComercioRequest;
import com.wilsontut.delivery.model.ComercioResponse;
import com.wilsontut.delivery.repository.ComercioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComercioService {

    private final ComercioRepository comercioRepository;

    @Transactional(readOnly = true)
    public List<ComercioResponse> listarComercios(CategoriaComercio categoria) {
        List<Comercio> comercios;
        if (categoria != null) {
            comercios = comercioRepository.findByAbiertoTrueAndCategoria(categoria);
        } else {
            comercios = comercioRepository.findByAbiertoTrue();
        }

        return comercios.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComercioResponse crearComercio(ComercioRequest request) {
        Comercio comercio = Comercio.builder()
                .nombre(request.getNombre())
                .categoria(request.getCategoria())
                .direccion(request.getDireccion())
                .abierto(request.getAbierto())
                .build();

        Comercio guardado = comercioRepository.save(comercio);
        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public Comercio obtenerComercioEntity(Long id) {
        return comercioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado con ID: " + id));
    }

    public ComercioResponse mapToResponse(Comercio comercio) {
        return ComercioResponse.builder()
                .id(comercio.getId())
                .nombre(comercio.getNombre())
                .categoria(comercio.getCategoria())
                .direccion(comercio.getDireccion())
                .abierto(comercio.getAbierto())
                .build();
    }
}
