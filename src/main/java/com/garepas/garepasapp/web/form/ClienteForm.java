package com.garepas.garepasapp.web.form;

import com.garepas.garepasapp.domain.command.ClienteCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulario de cliente (fiado, cobro y datos de contacto).
 */
public class ClienteForm {

    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    private String nombre;

    @Size(max = 30, message = "El telefono no puede superar los 30 caracteres")
    private String telefono;

    @Size(max = 150, message = "La direccion no puede superar los 150 caracteres")
    private String direccion;

    @Size(max = 250, message = "Las notas no pueden superar los 250 caracteres")
    private String notas;

    private Boolean activo = Boolean.TRUE;

    public ClienteCommand toCommand() {
        return new ClienteCommand(nombre, telefono, direccion, notas, activo);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
