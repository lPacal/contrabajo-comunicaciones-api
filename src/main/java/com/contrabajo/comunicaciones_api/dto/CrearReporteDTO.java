package com.contrabajo.comunicaciones_api.dto;

import lombok.Data;

@Data
public class CrearReporteDTO {
    private Integer idTipoReporte;
    private Long idOfertaServicio; // Se guardará en entidadId
    private Integer idUsuarioReportado; // El front lo manda, pero lo ignoramos en BD (se asume del dueño de la oferta)
    private Long idChatCita; // El front lo manda, pero lo ignoramos
    private String comentario; // Se guardará en descripcion
}