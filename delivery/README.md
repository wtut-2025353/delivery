# FastOrder API - Sistema de Delivery Backend

API RESTful desarrollada para la gestión de servicios de delivery, control de inventario de comercios, generación de pedidos en tiempo real y asignación de repartidores con seguridad basada en roles y tokens JWT.

## Tecnologias Utilizadas

- **Lenguaje:** Java 21
- **Framework Principal:** Spring Boot 3.2.2
- **Seguridad:** Spring Security con autenticación Stateless mediante JWT (io.jsonwebtoken)
- **Persistencia:** Spring Data JPA / Hibernate 6.4
- **Base de Datos:** MySQL Server 8.0
- **Gestor de Dependencias:** Apache Maven
- **Utilidades:** Lombok

## Requisitos Previos

- Java Development Kit (JDK) 17 o superior instalado y configurado en el PATH.
- MySQL Server 8.0 en ejecución en el puerto 3306.
- Git instalado (opcional, para ejecución de scripts bash).

## Configuracion de Base de Datos

1. Asegurarse de que el servicio de MySQL se encuentre en ejecución.
2. Crear la base de datos o verificar que el usuario tenga permisos suficientes. La aplicación creará automáticamente la base de datos `delivery_in5am` al iniciar si no existe.

Credenciales por defecto configuradas en `src/main/resources/application.properties`:

- URL: `jdbc:mysql://localhost:3306/delivery_in5am`
- Usuario: `IN5AM`
- Contraseña: `_odmon5Am`

En caso de requerir valores distintos, se pueden sobreescribir mediante variables de entorno:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PORT` (por defecto 8088)

## Pasos para Ejecutar el Proyecto

### 1. Clonar o descargar el repositorio
Abrir la terminal en la carpeta raíz del proyecto.

### 2. Compilar y verificar pruebas unitarias
Ejecutar el siguiente comando para validar que todas las pruebas pasen y se compilen los recursos:

En Windows (PowerShell / CMD):
```cmd
.\mvnw.cmd clean test
```

En Linux / macOS / Git Bash:
```bash
./mvnw clean test
```

### 3. Iniciar la aplicacion
Para arrancar el servidor backend en el puerto 8088:

En Windows:
```cmd
.\mvnw.cmd spring-boot:run
```

En Linux / macOS / Git Bash:
```bash
./mvnw spring-boot:run
```

El servidor estará listo cuando en la consola se muestre el mensaje `Started DeliveryApplication`.

## Autenticacion y Roles de Usuario

La seguridad de la API requiere un encabezado HTTP de autorización para los endpoints protegidos:
`Authorization: Bearer <TOKEN_JWT>`

Usuarios semilla creados por defecto al iniciar la base de datos (Contraseña para todos: `123456`):

1. **Administrador (`ADMIN`)**
   - Email: `admin@delivery.com`
   - Permisos: Registro de comercios y creación de productos.

2. **Repartidor (`REPARTIDOR`)**
   - Email: `repartidor@delivery.com`
   - Permisos: Visualización de pedidos disponibles y actualización de estado de entrega.

3. **Cliente (`CLIENTE`)**
   - Email: `cliente@delivery.com`
   - Permisos: Consulta de comercios, catálogo de productos, creación de pedidos y cancelación.

## Endpoints Principales

### Autenticacion (/api/v1/auth)
- `POST /api/v1/auth/register` - Registro público de clientes.
- `POST /api/v1/auth/login` - Autenticación y generación de token JWT.

### Comercios y Productos (/api/v1/comercios)
- `GET /api/v1/comercios` - Listar comercios activos (filtro opcional por `categoria`).
- `POST /api/v1/comercios` - Crear nuevo comercio (Solo ADMIN).
- `GET /api/v1/comercios/{id}/productos` - Consultar productos de un comercio.
- `POST /api/v1/comercios/{id}/productos` - Agregar producto a un comercio (Solo ADMIN).

### Pedidos (/api/v1/pedidos)
- `POST /api/v1/pedidos` - Crear pedido con cálculo automático de envío Q20.00 y descuento de stock (Solo CLIENTE).
- `GET /api/v1/pedidos/mis-pedidos` - Historial de pedidos del cliente autenticado (Solo CLIENTE).
- `GET /api/v1/pedidos/disponibles` - Listar pedidos pendientes de entrega (REPARTIDOR / ADMIN).
- `PATCH /api/v1/pedidos/{id}/estado` - Cambiar estado de envío del pedido (REPARTIDOR / ADMIN).
- `PATCH /api/v1/pedidos/{id}/cancelar` - Cancelar pedido en estado PENDIENTE y restaurar stock (CLIENTE / ADMIN).

## Pruebas de Carga y Rendimiento

El proyecto cuenta con un script para pruebas de estrés de alta concurrencia (500 peticiones con 50 hilos concurrentes):

En Git Bash / Linux:
```bash
./test-stress.sh
```

En PowerShell:
```powershell
.\test-stress.ps1
```

Los resultados consolidados por código HTTP se guardan automáticamente en `stress-results.txt`.
