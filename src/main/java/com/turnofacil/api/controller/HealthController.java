package com.turnofacil.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.SpringBootVersion;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Tag(name = "Sistema", description = "Endpoints de monitoreo del sistema")
public class HealthController {

    @Value("${spring.application.name:turnofacil-api}")
    private String appName;

    @Value("${spring.application.version:0.1.0}")
    private String appVersion;

    @GetMapping
    @Operation(summary = "Health check", description = "Verifica que la API está funcionando correctamente")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", appName);
        response.put("version", appVersion);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/liveness")
    @Operation(summary = "Liveness probe", description = "Kubernetes liveness probe endpoint")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "alive"));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Readiness probe", description = "Kubernetes readiness probe endpoint")
    public ResponseEntity<Map<String, String>> readiness() {
        return ResponseEntity.ok(Map.of("status", "ready"));
    }

    @GetMapping("/info")
    @Operation(summary = "Información de la aplicación", description = "Metadatos estáticos de la app")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(Map.of(
            "name", "TurnoFácil API",
            "version", "0.1.0",
            "description", "Microservicio para gestión de reservas de atención",
            "javaVersion", System.getProperty("java.version"),
            "springBootVersion", SpringBootVersion.getVersion()
        ));
    }
}