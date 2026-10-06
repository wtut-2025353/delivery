-- Inserción inicial de usuarios (Password: 123456 con hash BCrypt)
-- Evita duplicados al reiniciar usando INSERT IGNORE
INSERT IGNORE INTO usuarios (id, nombre, direccion, telefono, email, password, rol) VALUES
(1, 'Administrador General', 'Zona 1, Ciudad de Guatemala', '5555-1111', 'admin@delivery.com', '$2a$10$3echZIb4yKXSMHAUO.KmFe4HeTVmD3l1nHad4B4oEiQ0xMuRzLVje', 'ADMIN'),
(2, 'Carlos Repartidor', 'Zona 7, Ciudad de Guatemala', '5555-2222', 'repartidor@delivery.com', '$2a$10$3echZIb4yKXSMHAUO.KmFe4HeTVmD3l1nHad4B4oEiQ0xMuRzLVje', 'REPARTIDOR'),
(3, 'Juan Cliente', 'Zona 10, Ciudad de Guatemala', '5555-3333', 'cliente@delivery.com', '$2a$10$3echZIb4yKXSMHAUO.KmFe4HeTVmD3l1nHad4B4oEiQ0xMuRzLVje', 'CLIENTE');

-- Inserción de comercios
INSERT IGNORE INTO comercios (id, nombre, categoria, direccion, abierto) VALUES
(1, 'Burger King Las Américas', 'RESTAURANTE', 'Av. Las Américas 12-30, Zona 13', 1),
(2, 'Supermercado La Torre', 'SUPERMERCADO', 'Calzada Roosevelt 22-00, Zona 11', 1),
(3, 'Farmacia Galeno', 'FARMACIA', '6ta Avenida 4-12, Zona 1', 1);

-- Inserción de productos para los comercios
INSERT IGNORE INTO productos (id, comercio_id, nombre, precio, stock, disponible) VALUES
(1, 1, 'Hamburguesa Whopper Doble', 45.00, 50, 1),
(2, 1, 'Papas Fritas Medianas', 18.00, 100, 1),
(3, 1, 'Refresco Gaseosa 16oz', 12.00, 80, 1),
(4, 2, 'Leche Entera 1 Litro', 16.50, 40, 1),
(5, 2, 'Pan de Molde Blanco', 22.00, 30, 1),
(6, 3, 'Acetaminofén 500mg Caja 20u', 25.00, 60, 1),
(7, 3, 'Alcohol en Gel 250ml', 15.00, 45, 1);
