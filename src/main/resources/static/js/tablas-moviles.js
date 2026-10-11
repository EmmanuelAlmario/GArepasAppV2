/*
 * Tablas en tarjetas para pantallas pequenas.
 *
 * Copia el texto de cada encabezado (th) a data-etiqueta en la celda de la
 * misma columna; el CSS (layout.css) lo muestra como etiqueta antes del valor.
 * Asi las plantillas no repiten las etiquetas y el escritorio no cambia.
 * Las filas de "sin registros" (una sola celda con colspan) se marcan para
 * mostrarse como estado y no como tarjeta.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.table-responsive > table').forEach((tabla) => {
        const encabezados = Array.from(tabla.querySelectorAll('thead th'), (th) => th.textContent.trim());

        tabla.querySelectorAll('tbody tr').forEach((fila) => {
            const celdas = Array.from(fila.children);

            if (celdas.length === 1 && celdas[0].hasAttribute('colspan')) {
                fila.classList.add('ga-fila-vacia');
                return;
            }

            celdas.forEach((celda, indice) => {
                if (encabezados[indice]) {
                    celda.dataset.etiqueta = encabezados[indice];
                }
            });
        });
    });
});
