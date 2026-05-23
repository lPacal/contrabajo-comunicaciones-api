package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Solicitud para crear un reporte de moderacion sobre una oferta o interaccion.")
public class CrearReporteDTO {
    @Schema(description = "ID del tipo de reporte seleccionado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idTipoReporte;

    @Schema(description = "ID de la oferta de servicio reportada.", example = "100")
    private Long idOfertaServicio; // Se guardará en entidadId

    @Schema(description = "ID del usuario reportado enviado por frontend como referencia visual.", example = "8")
    private Integer idUsuarioReportado; // El front lo manda, pero lo ignoramos en BD (se asume del dueño de la oferta)

    @Schema(description = "ID del chat o cita asociada al reporte, si aplica.", example = "25")
    private Long idChatCita; // El front lo manda, pero lo ignoramos

    @Schema(description = "Comentario descriptivo del motivo del reporte.", example = "El trabajador no cumplio con lo acordado.")
    private String comentario; // Se guardará en descripcion
}
