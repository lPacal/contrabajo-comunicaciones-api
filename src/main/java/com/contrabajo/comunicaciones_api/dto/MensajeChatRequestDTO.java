package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Mensaje enviado dentro de un chat existente.")
public class MensajeChatRequestDTO {
    
    @Schema(description = "ID del chat donde se enviara el mensaje.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del chat es obligatorio.")
    private Long idChatOferta;

    @Schema(description = "Contenido visible del mensaje antes de ser cifrado para almacenamiento.", example = "Hola, podemos coordinar para manana.", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El mensaje no puede estar vacío.")
    private String contenido;

    // 0 = normal, 1 = sistema
    // Opcional para retrocompatibilidad con clientes antiguos.
    @Schema(description = "Tipo de mensaje: 0 normal, 1 sistema.", example = "0")
    private Integer tipo;
}
