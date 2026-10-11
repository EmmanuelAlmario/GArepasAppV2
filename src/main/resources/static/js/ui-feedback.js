/*
 * Feedback compartido para la interfaz Thymeleaf.
 *
 * 1. Confirmacion previa: cualquier form con data-confirm="mensaje" abre el
 *    modal Bootstrap (#modalConfirmacion) antes de enviarse.
 * 2. Estado de carga: al enviar un formulario se deshabilitan sus botones de
 *    submit para evitar dobles envios. Se puede omitir con data-sin-carga.
 */
document.addEventListener('DOMContentLoaded', () => {
    const modalEl = document.getElementById('modalConfirmacion');
    const mensajeEl = document.getElementById('modalConfirmacionMensaje');
    const aceptar = document.getElementById('modalConfirmacionAceptar');
    const modal = modalEl && window.bootstrap ? new bootstrap.Modal(modalEl) : null;

    let formPendiente = null;
    let disparador = null;

    if (modalEl && modal) {
        modalEl.addEventListener('hidden.bs.modal', () => {
            if (formPendiente) {
                delete formPendiente.dataset.confirmado;
                formPendiente = null;
            }
            if (disparador && typeof disparador.focus === 'function') {
                disparador.focus();
            }
        });
    }

    if (aceptar) {
        aceptar.addEventListener('click', () => {
            if (!formPendiente) return;
            const form = formPendiente;
            formPendiente = null;
            modal.hide();
            // requestSubmit dispara submit (y la validacion nativa) para que
            // el formulario siga el flujo normal con el estado de carga.
            form.requestSubmit();
        });
    }

    document.addEventListener('submit', (evento) => {
        const form = evento.target;
        if (!(form instanceof HTMLFormElement)) return;

        if (form.matches('[data-confirm]') && form.dataset.confirmado !== 'true' && modal) {
            evento.preventDefault();
            formPendiente = form;
            disparador = document.activeElement;
            form.dataset.confirmado = 'true';
            if (mensajeEl) {
                mensajeEl.textContent = form.dataset.confirm || '¿Confirmas esta accion?';
            }
            modal.show();
            return;
        }

        if (form.dataset.sinCarga !== undefined) return;

        form.querySelectorAll('button[type="submit"]:not([data-sin-carga]), input[type="submit"]:not([data-sin-carga])').forEach((boton) => {
            boton.disabled = true;
            if (boton.tagName === 'BUTTON') {
                boton.dataset.textoOriginal = boton.textContent;
                boton.innerHTML = '<span class="spinner-border spinner-border-sm me-1" aria-hidden="true"></span>Procesando...';
            }
        });
    });
});
