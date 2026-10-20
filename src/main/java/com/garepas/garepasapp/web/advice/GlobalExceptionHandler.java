package com.garepas.garepasapp.web.advice;

import com.garepas.garepasapp.domain.exception.OperacionInvalidaException;
import com.garepas.garepasapp.domain.exception.RecursoDuplicadoException;
import com.garepas.garepasapp.domain.exception.RecursoNoEncontradoException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Manejo global de errores de dominio.
 *
 * <p>Antes, una excepcion de negocio que escapaba de un controlador terminaba
 * en la pagina 500 (con el mensaje tecnico o vacio) y el usuario no entendia
 * que habia pasado ni que hacer. Ahora cada error de dominio vuelve a la
 * aplicacion con un mensaje util.
 *
 * <p>Los formularios validan con {@code @Valid} y los controladores capturan
 * las excepciones para re-renderizar el formulario; este advice es la red de
 * seguridad para lo que se escape de ahi.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public String noEncontrado(RecursoNoEncontradoException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensaje", ex.getMessage() + ".");
        redirect.addFlashAttribute("tipo", "danger");
        return "redirect:/";
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public String duplicado(RecursoDuplicadoException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensaje", ex.getMessage() + ".");
        redirect.addFlashAttribute("tipo", "warning");
        return "redirect:/";
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public String operacionInvalida(OperacionInvalidaException ex, RedirectAttributes redirect) {
        redirect.addFlashAttribute("mensaje", ex.getMessage());
        redirect.addFlashAttribute("tipo", "warning");
        return "redirect:/";
    }

    /**
     * Los errores de seguridad no se traducen a un mensaje: se relanzan para
     * que Spring los trate como corresponde (403 / reautenticacion).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public String accesoDenegado(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(AuthenticationException.class)
    public String noAutenticado(AuthenticationException ex) {
        throw ex;
    }

    /**
     * Cualquier otro error se registra completo en el log (para el que
     * administra) pero al usuario solo se le muestra un mensaje generico:
     * ni trazas ni detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public String inesperado(Exception ex, HttpServletResponse response, RedirectAttributes redirect) {
        log.error("Error no controlado en la aplicacion", ex);
        // Si la respuesta ya empezo a escribirse (por ejemplo, un fallo de
        // Thymeleaf a media pagina) no se puede redirigir: se deja el log y
        // no se intenta escribir nada mas sobre una respuesta comprometida.
        if (response.isCommitted()) {
            return null;
        }
        redirect.addFlashAttribute("mensaje",
                "Ocurrio un problema al guardar. Intenta de nuevo; si el problema continua, avisale al administrador.");
        redirect.addFlashAttribute("tipo", "danger");
        return "redirect:/";
    }
}
