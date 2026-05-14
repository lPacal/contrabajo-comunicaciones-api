package com.contrabajo.comunicaciones_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReporteResponseDTO {
    private Long idReporte;
    private Integer idEmisor;
    private Integer idUsuarioReportado; 
    private Long idOfertaServicio;
    private Long idChatCita; 
    private Integer idTipoReporte;
    private String comentario; 
    private LocalDateTime fechaCreacion;
    private String estadoRevision; 
    private Integer idModeradorRevisor; 
    private LocalDateTime fechaRevision; 
    private String medidaAplicada; 
    private String tipoReporteNombre;
    
    // --- Datos extraídos de otros Microservicios ---
    private String emisorUsername;
    private String usuarioReportadoUsername;
    private String usuarioReportadoNombre;
    private String servicioTitulo;
    private String servicioFotoUrl;
}