package com.contrabajo.comunicaciones_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MensajeChatRequestDTO {
    
    @NotNull(message = "El ID del chat es obligatorio.")
    private Long idChatOferta;

    @NotBlank(message = "El mensaje no puede estar vacío.")
    private String contenido;

    // 0 = normal, 1 = sistema
    // Opcional para retrocompatibilidad con clientes antiguos.
    private Integer tipo;
}
