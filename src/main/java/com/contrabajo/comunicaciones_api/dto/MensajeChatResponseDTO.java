package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Mensaje de chat devuelto al cliente con contenido descifrado para lectura.")
public class MensajeChatResponseDTO {
    @Schema(description = "ID unico del mensaje.", example = "100")
    private Long id;

    @Schema(description = "ID del chat al que pertenece el mensaje.", example = "10")
    private Long idChatOferta;

    @Schema(description = "ID del usuario emisor.", example = "1")
    private Integer idEmisor;

    @Schema(description = "ID del usuario receptor.", example = "2")
    private Integer idReceptor;

    @Schema(description = "Contenido del mensaje listo para mostrar en frontend.", example = "Hola, podemos coordinar para manana.")
    private String contenido;

    @Schema(description = "Fecha en que el mensaje fue enviado.")
    private LocalDateTime fechaEnvio;

    @Schema(description = "Fecha en que el mensaje fue marcado como recibido.", nullable = true)
    private LocalDateTime fechaRecibido;

    @Schema(description = "Fecha en que el mensaje fue marcado como leido.", nullable = true)
    private LocalDateTime fechaLeido;

    /** 0 = normal, 1 = sistema */
    @Schema(description = "Tipo de mensaje: 0 normal, 1 sistema.", example = "0")
    private Integer tipo;
}
