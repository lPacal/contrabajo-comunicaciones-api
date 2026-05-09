package com.contrabajo.comunicaciones_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatResponseDTO {
    private Long id;
    private Integer idTrabajador;
    private Integer idCliente;
    private Integer idOfertaServicio;
    private Integer idCita;         // null hasta que se vincule una cita
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    // Preview del ultimo mensaje (puede ser null si no hay mensajes)
    private String ultimoMensaje;
    private LocalDateTime fechaUltimoMensaje;

    // Cantidad de mensajes no leidos para el usuario que hizo el request
    private long mensajesNoLeidos;

    // Datos de visualizacion desnormalizados
    private String usernameTrabajador;
    private String usernameCliente;
    private String tituloServicio;
}
