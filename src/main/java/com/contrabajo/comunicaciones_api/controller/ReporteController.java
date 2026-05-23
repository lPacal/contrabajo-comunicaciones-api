package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.CrearReporteDTO;
import com.contrabajo.comunicaciones_api.dto.ReporteResponseDTO;
import com.contrabajo.comunicaciones_api.model.TipoReporte;
import com.contrabajo.comunicaciones_api.service.ReporteService;
import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Tag(name = "2. Reportes", description = "Endpoints para crear reportes y gestionar revision de moderacion")
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
    @Operation(summary = "Listar tipos de reporte", description = "Devuelve los tipos de reporte disponibles para que el cliente pueda clasificar una denuncia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de tipos de reporte.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TipoReporte.class)))
    })
    public ResponseEntity<?> obtenerTipos() {
        return ResponseEntity.ok(reporteService.obtenerTiposReporte());
    }

    @PostMapping
    @Operation(
            summary = "Crear reporte",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Registra un reporte emitido por el usuario autenticado sobre una oferta o interaccion. El emisor se obtiene desde el token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte creado correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Tipo de reporte invalido o error de negocio.",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Tipo de reporte inválido\"}"))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
    public ResponseEntity<?> crearReporte(@RequestBody CrearReporteDTO dto) {
        try {
            return ResponseEntity.ok(reporteService.crearReporte(obtenerIdDelToken(), dto));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('MODERADOR')")
    @Operation(
            summary = "Listar reportes para moderacion",
            description = "**Requiere rol MODERADOR (BearerAuth)**<br><br>" +
                    "Lista reportes enriquecidos con datos de usuarios y ofertas. Permite filtrar por busqueda, tipo, estado de revision y orden."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reportes.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Error al listar reportes.",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Error al componer reportes\"}"))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol MODERADOR.", content = @Content)
    })
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
    @Operation(
            summary = "Obtener detalle de reporte",
            description = "**Requiere rol MODERADOR (BearerAuth)**<br><br>" +
                    "Devuelve el detalle enriquecido de un reporte especifico para su revision."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte encontrado.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Reporte no encontrado o error de composicion.",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"Reporte no encontrado\"}"))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol MODERADOR.", content = @Content)
    })
    public ResponseEntity<?> obtenerDetalle(@PathVariable Long idReporte) {
        try {
            return ResponseEntity.ok(reporteService.obtenerDetalle(idReporte));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{idReporte}/revision")
    @PreAuthorize("hasRole('MODERADOR')")
    @Operation(
            summary = "Resolver reporte",
            description = "**Requiere rol MODERADOR (BearerAuth)**<br><br>" +
                    "Aplica una medida de moderacion y marca el reporte como resuelto. Medidas soportadas: `IGNORAR_REPORTE`, `DESACTIVAR_SERVICIO`, `ELIMINAR_SERVICIO`, `SUSPENDER_USUARIO_HASTA:{fecha}` y `BANEAR_USUARIO`."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reporte resuelto correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReporteResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Medida ausente, no soportada o fallo al aplicarla.",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"error\": \"La medida aplicada es obligatoria.\"}"))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "El usuario no posee rol MODERADOR.", content = @Content)
    })
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
