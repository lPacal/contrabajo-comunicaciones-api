package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Reporte enriquecido para visualizacion y revision de moderacion.")
public class ReporteResponseDTO {
    @Schema(description = "ID unico del reporte.", example = "10")
    private Long idReporte;

    @Schema(description = "ID del usuario que emitio el reporte.", example = "5")
    private Integer idEmisor;

    @Schema(description = "ID del usuario reportado resuelto desde la oferta.", example = "8", nullable = true)
    private Integer idUsuarioReportado; 

    @Schema(description = "ID de la oferta de servicio reportada.", example = "100")
    private Long idOfertaServicio;

    @Schema(description = "ID del chat o cita asociado al reporte, si aplica.", example = "25", nullable = true)
    private Long idChatCita; 

    @Schema(description = "ID del tipo de reporte.", example = "1")
    private Integer idTipoReporte;

    @Schema(description = "Comentario ingresado por el usuario denunciante.", example = "El servicio no correspondia a lo publicado.")
    private String comentario; 

    @Schema(description = "Fecha de creacion del reporte.")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Estado actual de revision del reporte.", example = "PENDIENTE")
    private String estadoRevision; 

    @Schema(description = "ID del moderador que reviso el reporte, si aplica.", example = "3", nullable = true)
    private Integer idModeradorRevisor; 

    @Schema(description = "Fecha en que se reviso el reporte, si aplica.", nullable = true)
    private LocalDateTime fechaRevision; 

    @Schema(description = "Medida de moderacion aplicada.", example = "DESACTIVAR_SERVICIO", nullable = true)
    private String medidaAplicada; 

    @Schema(description = "Nombre legible del tipo de reporte.", example = "Conducta inapropiada")
    private String tipoReporteNombre;
    
    // --- Datos extraídos de otros Microservicios ---
    @Schema(description = "Username del usuario emisor.", example = "cliente_prueba")
    private String emisorUsername;

    @Schema(description = "Username del usuario reportado.", example = "trabajador_prueba")
    private String usuarioReportadoUsername;

    @Schema(description = "Nombre completo del usuario reportado.", example = "Trabajador Prueba")
    private String usuarioReportadoNombre;

    @Schema(description = "Titulo de la oferta reportada.", example = "Reparacion de lavadora")
    private String servicioTitulo;

    @Schema(description = "URL de foto referencial de la oferta.", example = "https://cdn.contrabajo.local/ofertas/100.jpg")
    private String servicioFotoUrl;
}
