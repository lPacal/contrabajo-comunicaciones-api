package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensaje_soporte")
@Data
public class MensajeSoporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje_soporte")
    private Integer id;

    @Column(nullable = false, length = 80)
    private String asunto;

    @Column(nullable = false, length = 500)
    private String detalle;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio = LocalDateTime.now();

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "id_emisor", nullable = false)
    private Integer idEmisor;

    @Column(nullable = false)
    private Boolean resuelto = false;
}