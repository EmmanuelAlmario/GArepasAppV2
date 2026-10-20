  











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
          
        if (contenedor && contenedor.querySelectorAll('[data-fila]').length === 0) {
            const botonAgregar = document.querySelector('[data-agregar][data-contenedor="#' + contenedor.id + '"]');
            if (botonAgregar) botonAgregar.click();
        }
    }
});
