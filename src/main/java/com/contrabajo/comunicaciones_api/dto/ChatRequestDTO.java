package com.contrabajo.comunicaciones_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequestDTO {

    @NotNull(message = "El ID del trabajador es obligatorio.")
    private Integer idTrabajador;

    @NotNull(message = "El ID de la oferta de servicio es obligatorio.")
    private Integer idOfertaServicio;

    // Opcionales — datos de visualizacion que el cliente envia para evitar joins posteriores
    private String usernameTrabajador;
    private String usernameCliente;
    private String tituloServicio;
}