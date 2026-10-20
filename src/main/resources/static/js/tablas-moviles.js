  








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
