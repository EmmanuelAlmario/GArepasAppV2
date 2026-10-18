/*
 * Carrito del punto de venta (UC21).
 *
 * - Toma los productos del catalogo mediante atributos data-*.
 * - Mantiene el pedido en memoria y lo dibuja en #carrito con nodos seguros
 *   (textContent; nunca innerHTML con datos del servidor).
 * - Filtra el catalogo por nombre.
 * - En movil el pedido es una hoja inferior: #pedidoToggle la abre y la cierra.
 * - Si el servidor devuelve un error, reconstruye el pedido desde
 *   window.carritoInicial para no perder el trabajo del usuario.
 * - Al enviar el formulario agrega campos ocultos repetidos
 *   (productoId / cantidad), que Spring enlaza como listas.
 */
document.addEventListener('DOMContentLoaded', () => {
    const catalogo = document.getElementById('catalogo');
    const lista = document.getElementById('carrito');
    const vacio = document.getElementById('carritoVacio');
    const totalEl = document.getElementById('totalCarrito');
    const cantidadEl = document.getElementById('pedidoCantidad');
    const boton = document.getElementById('botonCobrar');
    const form = document.getElementById('formVenta');
    const campos = document.getElementById('camposCarrito');
    const mensaje = document.getElementById('posMensaje');
    const filtro = document.getElementById('filtroProductos');
    const sinResultados = document.getElementById('sinResultadosCatalogo');
    const pedido = document.getElementById('pedido');
    const abrir = document.getElementById('pedidoToggle');

    if (!catalogo || !form) return;

    const carrito = new Map();
    const moneda = new Intl.NumberFormat('es-CO', {
        style: 'currency', currency: 'COP', maximumFractionDigits: 0
    });

    // El contenedor del mensaje nunca se oculta: asi los lectores de pantalla anuncian los cambios.
    function mostrarMensaje(texto, tipo) {
        if (!mensaje) return;
        mensaje.textContent = texto;
        mensaje.className = 'ga-pos__aviso alert alert-' + (tipo || 'warning') + ' py-2 small mb-2';
    }

    function limpiarMensaje() {
        if (!mensaje) return;
        mensaje.textContent = '';
        mensaje.className = 'ga-pos__aviso';
    }

    function total() {
        let suma = 0;
        carrito.forEach((item) => { suma += item.precio * item.cantidad; });
        return suma;
    }

    function unidades() {
        let suma = 0;
        carrito.forEach((item) => { suma += item.cantidad; });
        return suma;
    }

    function botonCantidad(etiqueta, accion, id, clase) {
        const control = document.createElement('button');
        control.type = 'button';
        control.className = clase || 'ga-qty';
        control.textContent = etiqueta;
        control.dataset.accion = accion;
        control.dataset.id = id;
        return control;
    }

    function render() {
        lista.innerHTML = '';
        carrito.forEach((item, id) => {
            const li = document.createElement('li');
            li.className = 'ga-linea';

            const info = document.createElement('div');
            info.className = 'ga-linea__info';

            const nombre = document.createElement('div');
            nombre.className = 'ga-linea__nombre';
            nombre.textContent = item.nombre;

            const precio = document.createElement('div');
            precio.className = 'ga-linea__precio';
            precio.textContent = moneda.format(item.precio) + ' c/u';

            info.append(nombre, precio);

            const menos = botonCantidad('−', 'menos', id);
            menos.setAttribute('aria-label', 'Quitar una unidad de ' + item.nombre);

            const cantidad = document.createElement('span');
            cantidad.className = 'ga-linea__cantidad';
            cantidad.textContent = item.cantidad;

            const mas = botonCantidad('+', 'mas', id);
            mas.setAttribute('aria-label', 'Agregar una unidad de ' + item.nombre);

            const controles = document.createElement('div');
            controles.className = 'ga-linea__controles';
            controles.append(menos, cantidad, mas);

            const quitar = botonCantidad('×', 'quitar', id, 'ga-qty ga-qty--quitar');
            quitar.setAttribute('aria-label', 'Quitar ' + item.nombre + ' del pedido');

            li.append(info, controles, quitar);
            lista.appendChild(li);
        });

        const hayItems = carrito.size > 0;
        vacio.style.display = hayItems ? 'none' : '';
        boton.disabled = !hayItems;
        totalEl.textContent = moneda.format(total());
        cantidadEl.textContent = unidades();
    }

    function bajoPedido(productoDate) {
        return productoDate && productoDate.dataset.bajoPedido === 'true';
    }

    function agregar(producto) {
        const id = producto.dataset.id;
        const existente = carrito.get(id);
        const stock = Number(producto.dataset.stock);
        const cantidadActual = existente ? existente.cantidad : 0;

        if (!bajoPedido(producto) && cantidadActual >= stock) {
            mostrarMensaje('Stock insuficiente para ' + producto.dataset.nombre + '.', 'warning');
            return;
        }
        limpiarMensaje();
        if (existente) {
            existente.cantidad += 1;
        } else {
            carrito.set(id, {
                nombre: producto.dataset.nombre,
                precio: Number(producto.dataset.precio),
                cantidad: 1,
                stock: stock,
                bajoPedido: bajoPedido(producto)
            });
        }
        render();
    }

    catalogo.addEventListener('click', (evento) => {
        const botonProducto = evento.target.closest('[data-producto]');
        if (botonProducto) agregar(botonProducto);
    });

    lista.addEventListener('click', (evento) => {
        const accion = evento.target.closest('[data-accion]');
        if (!accion) return;

        const id = accion.dataset.id;
        const item = carrito.get(id);
        if (!item) return;

        if (accion.dataset.accion === 'mas') {
            if (!item.bajoPedido && item.cantidad >= item.stock) {
                mostrarMensaje('Stock insuficiente para ' + item.nombre + '.', 'warning');
                return;
            }
            item.cantidad += 1;
        } else if (accion.dataset.accion === 'menos') {
            item.cantidad -= 1;
            if (item.cantidad <= 0) carrito.delete(id);
        } else if (accion.dataset.accion === 'quitar') {
            carrito.delete(id);
        }
        limpiarMensaje();
        render();
    });

    if (filtro) {
        filtro.addEventListener('input', () => {
            const texto = filtro.value.trim().toLowerCase();
            let visibles = 0;
            catalogo.querySelectorAll('[data-producto]').forEach((producto) => {
                const columna = producto.closest('[data-producto-col]');
                if (!columna) return;
                const coincide = texto === '' || producto.dataset.nombre.toLowerCase().includes(texto);
                columna.hidden = !coincide;
                if (coincide) visibles += 1;
            });
            if (sinResultados) sinResultados.classList.toggle('d-none', visibles !== 0);
        });
    }

    // En movil, el resumen del pedido abre y cierra la hoja inferior.
    if (abrir && pedido) {
        abrir.addEventListener('click', () => {
            const abierto = pedido.classList.toggle('is-abierto');
            abrir.setAttribute('aria-expanded', String(abierto));
        });
    }

    form.addEventListener('submit', () => {
        campos.innerHTML = '';
        carrito.forEach((item, id) => {
            const campoId = document.createElement('input');
            campoId.type = 'hidden';
            campoId.name = 'productoId';
            campoId.value = id;
            campos.appendChild(campoId);

            const campoCantidad = document.createElement('input');
            campoCantidad.type = 'hidden';
            campoCantidad.name = 'cantidad';
            campoCantidad.value = item.cantidad;
            campos.appendChild(campoCantidad);
        });
        // El boton de cobro esta en la cabecera del pedido, fuera del formulario:
        // se bloquea aqui para evitar dobles envios.
        boton.disabled = true;
        boton.textContent = 'Registrando...';
    });

    // Pedido conservado por el servidor tras un error de validacion.
    if (Array.isArray(window.carritoInicial)) {
        window.carritoInicial.forEach((dato) => {
            if (!dato || dato.id == null) return;
            carrito.set(String(dato.id), {
                nombre: dato.nombre,
                precio: Number(dato.precio),
                cantidad: Number(dato.cantidad),
                stock: Number(dato.stock),
                bajoPedido: dato.bajoPedido === true
            });
        });
    }

    render();
});
