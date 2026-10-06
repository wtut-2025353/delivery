#!/bin/bash
# ==============================================================================
# Script de Pruebas Automatizadas: test-fastorder.sh
# Sistema de Delivery y Gestión de Pedidos - API REST
# NOTA: Requiere que la aplicación Spring Boot esté corriendo en http://localhost:8080
# ==============================================================================

BASE_URL="${BASE_URL:-http://localhost:8088/api/v1}"
FAILED_TESTS=0
TOTAL_TESTS=0

echo "======================================================================"
echo "    INICIANDO PRUEBAS DE INTEGRACIÓN: API REST DELIVERY (5to Perito)"
echo "======================================================================"
echo ""

run_test() {
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    local test_name="$1"
    local expected_code="$2"
    local actual_code="$3"
    local response_body="$4"

    if [ "$actual_code" -eq "$expected_code" ]; then
        echo -e "[\e[32mPASS\e[0m] $test_name (HTTP $actual_code)"
    else
        echo -e "[\e[31mFAIL\e[0m] $test_name (Esperado: HTTP $expected_code, Recibido: HTTP $actual_code)"
        echo "       Respuesta: $response_body"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
}

# 1. Login ADMIN
echo "--- 1. Autenticación ---"
RESP_ADMIN=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@delivery.com","password":"123456"}')
HTTP_CODE=$(echo "$RESP_ADMIN" | tail -n1)
BODY=$(echo "$RESP_ADMIN" | sed '$d')
ADMIN_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')
run_test "Login como ADMIN" 200 "$HTTP_CODE" "$BODY"

# 2. Login REPARTIDOR
RESP_REP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"repartidor@delivery.com","password":"123456"}')
HTTP_CODE=$(echo "$RESP_REP" | tail -n1)
BODY=$(echo "$RESP_REP" | sed '$d')
REP_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')
run_test "Login como REPARTIDOR" 200 "$HTTP_CODE" "$BODY"

# 3. Registro nuevo CLIENTE
RANDOM_NUM=$((1000 + RANDOM % 9000))
NEW_CLIENT_EMAIL="cliente_${RANDOM_NUM}@delivery.com"
RESP_REG=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"nombre\":\"Cliente Prueba\",\"direccion\":\"Zona 10\",\"telefono\":\"55554444\",\"email\":\"$NEW_CLIENT_EMAIL\",\"password\":\"123456\"}")
HTTP_CODE=$(echo "$RESP_REG" | tail -n1)
BODY=$(echo "$RESP_REG" | sed '$d')
CLIENT_TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | grep -o '[^"]*$')
run_test "Registro nuevo CLIENTE" 201 "$HTTP_CODE" "$BODY"

# 4. Intento de acceso sin JWT a comercios (Esperado 401)
echo ""
echo "--- 2. Seguridad y Control de Acceso ---"
RESP_NO_AUTH=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/comercios")
HTTP_CODE=$(echo "$RESP_NO_AUTH" | tail -n1)
BODY=$(echo "$RESP_NO_AUTH" | sed '$d')
run_test "Acceso sin Token a /comercios (Esperado 401)" 401 "$HTTP_CODE" "$BODY"

# 5. Listar Comercios autenticado como CLIENTE (Esperado 200)
RESP_COMERCIOS=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/comercios" \
  -H "Authorization: Bearer $CLIENT_TOKEN")
HTTP_CODE=$(echo "$RESP_COMERCIOS" | tail -n1)
BODY=$(echo "$RESP_COMERCIOS" | sed '$d')
run_test "Listar Comercios abiertos con Token CLIENTE" 200 "$HTTP_CODE" "$BODY"

# 6. Intento de crear comercio como CLIENTE (Esperado 403)
RESP_FORBIDDEN=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/comercios" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Tienda No Autorizada","categoria":"RESTAURANTE","direccion":"Zona 1","abierto":true}')
HTTP_CODE=$(echo "$RESP_FORBIDDEN" | tail -n1)
BODY=$(echo "$RESP_FORBIDDEN" | sed '$d')
run_test "Crear comercio como CLIENTE (Esperado 403 Forbidden)" 403 "$HTTP_CODE" "$BODY"

# 7. Crear Comercio como ADMIN (Esperado 201)
echo ""
echo "--- 3. Gestión de Catálogo (ADMIN) ---"
RESP_NEW_COM=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/comercios" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Pizzería Don Bosco","categoria":"RESTAURANTE","direccion":"Zona 8, Kinal","abierto":true}')
HTTP_CODE=$(echo "$RESP_NEW_COM" | tail -n1)
BODY=$(echo "$RESP_NEW_COM" | sed '$d')
COMERCIO_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)
run_test "Crear Comercio como ADMIN" 201 "$HTTP_CODE" "$BODY"

# 8. Agregar Producto como ADMIN (Esperado 201)
RESP_NEW_PROD=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/comercios/$COMERCIO_ID/productos" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Pizza Pepperoni Grande","precio":65.00,"stock":10,"disponible":true}')
HTTP_CODE=$(echo "$RESP_NEW_PROD" | tail -n1)
BODY=$(echo "$RESP_NEW_PROD" | sed '$d')
PRODUCTO_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)
run_test "Agregar Producto a Comercio como ADMIN" 201 "$HTTP_CODE" "$BODY"

# 9. Listar productos del comercio
RESP_PROD_LIST=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/comercios/$COMERCIO_ID/productos" \
  -H "Authorization: Bearer $CLIENT_TOKEN")
HTTP_CODE=$(echo "$RESP_PROD_LIST" | tail -n1)
BODY=$(echo "$RESP_PROD_LIST" | sed '$d')
run_test "Listar Productos del Comercio" 200 "$HTTP_CODE" "$BODY"

# 10. Crear Pedido como CLIENTE (2 pizzas @ Q65 = Q130 + Q20 envío = Q150)
echo ""
echo "--- 4. Creación de Pedido y Transacciones ---"
RESP_PEDIDO=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/pedidos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productoId\":$PRODUCTO_ID,\"cantidad\":2}]}")
HTTP_CODE=$(echo "$RESP_PEDIDO" | tail -n1)
BODY=$(echo "$RESP_PEDIDO" | sed '$d')
PEDIDO_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)
run_test "Crear Pedido (Cálculo Q130 + Q20 envío = Q150)" 201 "$HTTP_CODE" "$BODY"

# 11. Intentar Pedido con stock insuficiente (Esperado 409)
RESP_OVERSTOCK=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/pedidos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productoId\":$PRODUCTO_ID,\"cantidad\":500}]}")
HTTP_CODE=$(echo "$RESP_OVERSTOCK" | tail -n1)
BODY=$(echo "$RESP_OVERSTOCK" | sed '$d')
run_test "Pedido con Stock Insuficiente (Esperado 409 Conflict)" 409 "$HTTP_CODE" "$BODY"

# 12. Mis pedidos como CLIENTE
RESP_MIS_PEDIDOS=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pedidos/mis-pedidos" \
  -H "Authorization: Bearer $CLIENT_TOKEN")
HTTP_CODE=$(echo "$RESP_MIS_PEDIDOS" | tail -n1)
BODY=$(echo "$RESP_MIS_PEDIDOS" | sed '$d')
run_test "Consultar Mis Pedidos (Historial del Cliente)" 200 "$HTTP_CODE" "$BODY"

# 13. Pedidos disponibles como REPARTIDOR
echo ""
echo "--- 5. Flujo de Estados y Cancelación ---"
RESP_DISP=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/pedidos/disponibles" \
  -H "Authorization: Bearer $REP_TOKEN")
HTTP_CODE=$(echo "$RESP_DISP" | tail -n1)
BODY=$(echo "$RESP_DISP" | sed '$d')
run_test "Ver Pedidos Disponibles como REPARTIDOR" 200 "$HTTP_CODE" "$BODY"

# 14. Cambiar estado a EN_PREPARACION
RESP_ESTADO_PREP=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/pedidos/$PEDIDO_ID/estado" \
  -H "Authorization: Bearer $REP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_PREPARACION"}')
HTTP_CODE=$(echo "$RESP_ESTADO_PREP" | tail -n1)
BODY=$(echo "$RESP_ESTADO_PREP" | sed '$d')
run_test "Cambiar Estado a EN_PREPARACION" 200 "$HTTP_CODE" "$BODY"

# 15. Cambiar estado a EN_CAMINO
RESP_ESTADO_CAM=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/pedidos/$PEDIDO_ID/estado" \
  -H "Authorization: Bearer $REP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"EN_CAMINO"}')
HTTP_CODE=$(echo "$RESP_ESTADO_CAM" | tail -n1)
BODY=$(echo "$RESP_ESTADO_CAM" | sed '$d')
run_test "Cambiar Estado a EN_CAMINO" 200 "$HTTP_CODE" "$BODY"

# 16. Cambiar estado a ENTREGADO
RESP_ESTADO_ENT=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/pedidos/$PEDIDO_ID/estado" \
  -H "Authorization: Bearer $REP_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"estado":"ENTREGADO"}')
HTTP_CODE=$(echo "$RESP_ESTADO_ENT" | tail -n1)
BODY=$(echo "$RESP_ESTADO_ENT" | sed '$d')
run_test "Cambiar Estado a ENTREGADO" 200 "$HTTP_CODE" "$BODY"

# 17. Crear segundo pedido para probar Cancelación y Restauración de Stock
RESP_PEDIDO2=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/pedidos" \
  -H "Authorization: Bearer $CLIENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productoId\":$PRODUCTO_ID,\"cantidad\":1}]}")
HTTP_CODE=$(echo "$RESP_PEDIDO2" | tail -n1)
BODY=$(echo "$RESP_PEDIDO2" | sed '$d')
PEDIDO2_ID=$(echo "$BODY" | grep -o '"id":[0-9]*' | head -n1 | cut -d: -f2)

# Cancelar pedido en estado PENDIENTE
RESP_CANCEL=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/pedidos/$PEDIDO2_ID/cancelar" \
  -H "Authorization: Bearer $CLIENT_TOKEN")
HTTP_CODE=$(echo "$RESP_CANCEL" | tail -n1)
BODY=$(echo "$RESP_CANCEL" | sed '$d')
run_test "Cancelar Pedido PENDIENTE y Restaurar Stock" 200 "$HTTP_CODE" "$BODY"

echo ""
echo "======================================================================"
echo " RESUMEN: Total: $TOTAL_TESTS | Exitosas: $((TOTAL_TESTS - FAILED_TESTS)) | Fallidas: $FAILED_TESTS"
echo "======================================================================"

if [ "$FAILED_TESTS" -ne 0 ]; then
    exit 1
fi
exit 0
