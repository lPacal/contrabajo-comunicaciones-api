package com.contrabajo.comunicaciones_api.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MensajeChatResponseDTO {
    private Long id;
    private Long idChatOferta;
    private Integer idEmisor;
    private Integer idReceptor;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private LocalDateTime fechaRecibido;
    private LocalDateTime fechaLeido;
    /** 0 = normal, 1 = sistema */
    private Integer tipo;
}