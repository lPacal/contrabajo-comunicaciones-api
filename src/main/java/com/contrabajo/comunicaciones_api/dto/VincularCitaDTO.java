package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Solicitud para vincular una cita creada en servicios-api con un chat.")
public class VincularCitaDTO {
    @Schema(description = "ID de la cita que se asociara al chat.", example = "77", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer idCita;
}
