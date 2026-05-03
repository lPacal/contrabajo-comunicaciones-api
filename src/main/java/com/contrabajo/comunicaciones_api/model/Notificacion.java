package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Data
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false, length = 200)
    private String detalle;

    @Column(name = "id_usuario_receptor", nullable = false)
    private Integer idUsuarioReceptor;

    @Column(nullable = false)
    private Boolean leida = false;

    @Column(name = "url_destino", length = 300)
    private String urlDestino;
}