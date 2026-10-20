package com.garepas.garepasapp.web.view;

import com.garepas.garepasapp.domain.model.Producto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regla de disponibilidad del menu publico: igual que en el punto de venta.
 * Bajo pedido siempre disponible; los demas solo con stock terminado.
 */
class MenuPublicoItemResponseTest {

    private Producto producto(int stock, Boolean bajoPedido) {
        return Producto.builder()
                .id(7L)
                .nombre("Arepa de queso")
                .stockActual(stock)
                .precioVenta(new BigDecimal("3500"))
                .bajoPedido(bajoPedido)
                .activo(true)
                .build();
    }

    @Test
    void productoConStockEstaDisponible() {
        MenuPublicoItemResponse item = MenuPublicoItemResponse.desde(producto(5, false));
        assertTrue(item.disponible());
        assertEquals(7L, item.id());
        assertEquals("Arepa de queso", item.nombre());
        assertEquals(new BigDecimal("3500"), item.precio());
    }

    @Test
    void productoSinStockNoEstaDisponible() {
        assertFalse(MenuPublicoItemResponse.desde(producto(0, false)).disponible());
    }

    @Test
    void productoBajoPedidoEstaDisponibleAunSinStock() {
        assertTrue(MenuPublicoItemResponse.desde(producto(0, true)).disponible());
    }

    @Test
    void bajoPedidoNulloSeTrataComoFalso() {
        assertFalse(MenuPublicoItemResponse.desde(producto(0, null)).disponible());
    }
}
