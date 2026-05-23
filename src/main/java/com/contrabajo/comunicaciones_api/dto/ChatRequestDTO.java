package com.contrabajo.comunicaciones_api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Datos necesarios para iniciar o recuperar un chat asociado a una oferta de servicio.")
public class ChatRequestDTO {

    @Schema(description = "ID del trabajador dueño de la oferta de servicio.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID del trabajador es obligatorio.")
    private Integer idTrabajador;

    @Schema(description = "ID de la oferta de servicio asociada al chat.", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El ID de la oferta de servicio es obligatorio.")
    private Integer idOfertaServicio;

    // Opcionales — datos de visualizacion que el cliente envia para evitar joins posteriores
    @Schema(description = "Nombre de usuario del trabajador para visualizacion del chat.", example = "trabajador_prueba")
    private String usernameTrabajador;

    @Schema(description = "Nombre de usuario del cliente para visualizacion del chat.", example = "cliente_prueba")
    private String usernameCliente;

    @Schema(description = "Titulo de la oferta de servicio para visualizacion del chat.", example = "Reparacion de lavadora")
    private String tituloServicio;
}
