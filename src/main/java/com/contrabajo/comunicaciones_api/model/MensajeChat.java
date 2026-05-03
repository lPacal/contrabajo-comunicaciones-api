package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_chat")
@Data
public class MensajeChat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje_chat")
    private Long id;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "fecha_recibido")
    private LocalDateTime fechaRecibido;

    @Column(name = "fecha_leido")
    private LocalDateTime fechaLeido;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(name = "id_emisor", nullable = false)
    private Integer idEmisor;

    @Column(name = "id_receptor", nullable = false)
    private Integer idReceptor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_chat_oferta", nullable = false)
    private ChatOferta chatOferta;
}