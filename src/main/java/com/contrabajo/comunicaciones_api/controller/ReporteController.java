package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.CrearReporteDTO;
import com.contrabajo.comunicaciones_api.service.ReporteService;
import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request;

    private Integer obtenerIdDelToken() {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.extractId(token);
    }

    @GetMapping("/tipos")
    public ResponseEntity<?> obtenerTipos() {
        return ResponseEntity.ok(reporteService.obtenerTiposReporte());
    }

    @PostMapping
    public ResponseEntity<?> crearReporte(@RequestBody CrearReporteDTO dto) {
        try {
            return ResponseEntity.ok(reporteService.crearReporte(obtenerIdDelToken(), dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MODERADOR')")
    public ResponseEntity<?> listarReportes(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estadoRevision,
            @RequestParam(required = false) Integer idTipoReporte,
            @RequestParam(defaultValue = "true") Boolean ordenarRecientes) {
        try {
            return ResponseEntity.ok(reporteService.listarReportes(busqueda, idTipoReporte, estadoRevision, ordenarRecientes));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{idReporte}")
    @PreAuthorize("hasRole('MODERADOR')")
    public ResponseEntity<?> obtenerDetalle(@PathVariable Long idReporte) {
        try {
            return ResponseEntity.ok(reporteService.obtenerDetalle(idReporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{idReporte}/revision")
    @PreAuthorize("hasRole('MODERADOR')")
    public ResponseEntity<?> revisarReporte(@PathVariable Long idReporte, @RequestBody Map<String, String> payload) {
        try {
            String accion = payload.get("medidaAplicada"); 
            if (accion == null || accion.trim().isEmpty()) {
                throw new RuntimeException("La medida aplicada es obligatoria.");
            }
            return ResponseEntity.ok(reporteService.resolverReporte(idReporte, accion));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}