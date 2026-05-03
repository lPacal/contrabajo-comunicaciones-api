package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_oferta")
@Data
public class ChatOferta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_chat_oferta")
    private Long id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "id_trabajador", nullable = false)
    private Integer idTrabajador;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "id_oferta_servicio", nullable = false)
    private Integer idOfertaServicio;

    @Column(nullable = false)
    private Boolean activo = true;
}