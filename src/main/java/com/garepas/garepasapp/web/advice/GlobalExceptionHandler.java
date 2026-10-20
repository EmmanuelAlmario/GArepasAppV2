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

      



    @ExceptionHandler(AccessDeniedException.class)
    public String accesoDenegado(AccessDeniedException ex) {
        throw ex;
    }

    @ExceptionHandler(AuthenticationException.class)
    public String noAutenticado(AuthenticationException ex) {
        throw ex;
    }

      




    @ExceptionHandler(Exception.class)
    public String inesperado(Exception ex, HttpServletResponse response, RedirectAttributes redirect) {
        log.error("Error no controlado en la aplicacion", ex);
          
          
          
        if (response.isCommitted()) {
            return null;
        }
        redirect.addFlashAttribute("mensaje",
                "Ocurrio un problema al guardar. Intenta de nuevo; si el problema continua, avisale al administrador.");
        redirect.addFlashAttribute("tipo", "danger");
        return "redirect:/";
    }
}
