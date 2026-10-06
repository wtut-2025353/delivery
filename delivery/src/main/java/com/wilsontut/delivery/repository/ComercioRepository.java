package com.wilsontut.delivery.repository;

import com.wilsontut.delivery.entity.Comercio;
import com.wilsontut.delivery.enums.CategoriaComercio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    List<Comercio> findByAbiertoTrue();
    List<Comercio> findByAbiertoTrueAndCategoria(CategoriaComercio categoria);
}
