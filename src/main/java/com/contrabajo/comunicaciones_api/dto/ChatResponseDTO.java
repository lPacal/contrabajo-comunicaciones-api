package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Resumen de chat mostrado al usuario autenticado.")
public class ChatResponseDTO {
    @Schema(description = "ID unico del chat.", example = "10")
    private Long id;

    @Schema(description = "ID del trabajador participante.", example = "2")
    private Integer idTrabajador;

    @Schema(description = "ID del cliente participante.", example = "1")
    private Integer idCliente;

    @Schema(description = "ID de la oferta de servicio relacionada.", example = "50")
    private Integer idOfertaServicio;

    @Schema(description = "ID de la cita vinculada al chat, si existe.", example = "77", nullable = true)
    private Integer idCita;         // null hasta que se vincule una cita

    @Schema(description = "Indica si el chat permite nuevos mensajes.", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de creacion del chat.")
    private LocalDateTime fechaCreacion;

    // Preview del ultimo mensaje (puede ser null si no hay mensajes)
    @Schema(description = "Contenido del ultimo mensaje visible para previsualizacion.", example = "Hola, coordinemos el horario.", nullable = true)
    private String ultimoMensaje;

    @Schema(description = "Fecha del ultimo mensaje del chat.", nullable = true)
    private LocalDateTime fechaUltimoMensaje;

    // Cantidad de mensajes no leidos para el usuario que hizo el request
    @Schema(description = "Cantidad de mensajes no leidos para el usuario autenticado.", example = "3")
    private long mensajesNoLeidos;

    // Datos de visualizacion desnormalizados
    @Schema(description = "Nombre de usuario del trabajador.", example = "trabajador_prueba")
    private String usernameTrabajador;

    @Schema(description = "Nombre de usuario del cliente.", example = "cliente_prueba")
    private String usernameCliente;

    @Schema(description = "Titulo de la oferta asociada.", example = "Reparacion de lavadora")
    private String tituloServicio;
}
