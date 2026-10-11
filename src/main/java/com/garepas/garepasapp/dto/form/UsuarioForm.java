package com.garepas.garepasapp.dto.form;

import com.garepas.garepasapp.dto.request.UsuarioRequest;
import com.garepas.garepasapp.enums.Rol;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UsuarioForm {

    private Long id;

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no puede superar 50 caracteres")
    private String username;

    /** Obligatoria al crear; opcional al editar (vacia = no cambiar). */
    @Size(min = 6, max = 100, message = "La contrasena debe tener al menos 6 caracteres")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Rol rol;

    private Boolean activo = Boolean.TRUE;

    public UsuarioRequest toRequest() {
        return new UsuarioRequest(
                username,
                (password == null || password.isBlank()) ? null : password,
                rol,
                activo == null ? Boolean.TRUE : activo);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}