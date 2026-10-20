package com.garepas.garepasapp.api;

import com.garepas.garepasapp.web.view.MenuPublicoItemResponse;
import com.garepas.garepasapp.domain.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

  






@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicMenuController {

    private final ProductoService productoService;

    @GetMapping("/menu")
    public ResponseEntity<List<MenuPublicoItemResponse>> menu() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(productoService.listarMenuPublico());
    }
}
