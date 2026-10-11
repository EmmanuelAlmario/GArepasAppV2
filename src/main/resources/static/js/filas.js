/*
 * Filas dinamicas para formularios Thymeleaf.
 * Uso:
 *   <button data-agregar="#plantillaX" data-contenedor="#listaX">+ Agregar</button>
 *   <div id="listaX"> ...filas iniciales... </div>
 *   <template id="plantillaX"> <div data-fila> ... <button data-quitar>Quitar</button> ... </div> </template>
 *
 * Las filas clonadas conservan los mismos nombres de campo repetidos
 * (name="insumoId", name="cantidad", ...), que Spring enlaza como listas.
 * Tras agregar una fila se lleva el foco a su primer control para no perder
 * el contexto en teclado o lectores de pantalla.
 */
document.addEventListener('click', (evento) => {
    const agregar = evento.target.closest('[data-agregar]');
    if (agregar) {
        const plantilla = document.querySelector(agregar.dataset.agregar);
        const contenedor = document.querySelector(agregar.dataset.contenedor);
        if (plantilla && contenedor) {
            contenedor.appendChild(plantilla.content.cloneNode(true));
            const filas = contenedor.querySelectorAll('[data-fila]');
            const ultima = filas[filas.length - 1];
            const foco = ultima ? ultima.querySelector('select, input') : null;
            if (foco) foco.focus();
        }
        return;
    }

    const quitar = evento.target.closest('[data-quitar]');
    if (quitar) {
        const fila = quitar.closest('[data-fila]');
        const contenedor = fila ? fila.parentElement : null;
        if (fila) fila.remove();
        // Si el contenedor queda vacio se conserva una fila para poder seguir operando.
        if (contenedor && contenedor.querySelectorAll('[data-fila]').length === 0) {
            const botonAgregar = document.querySelector('[data-agregar][data-contenedor="#' + contenedor.id + '"]');
            if (botonAgregar) botonAgregar.click();
        }
    }
});
