package com.wilsontut.delivery.repository;

import com.wilsontut.delivery.entity.Pedido;
import com.wilsontut.delivery.entity.Usuario;
import com.wilsontut.delivery.enums.EstadoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByClienteOrderByFechaPedidoDesc(Usuario cliente);
    List<Pedido> findByClienteIdOrderByFechaPedidoDesc(Long clienteId);
    List<Pedido> findByEstadoInOrderByFechaPedidoDesc(List<EstadoPedido> estados);
}
