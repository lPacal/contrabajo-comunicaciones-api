package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte")
@Data
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Long id;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(name = "descripcion_reporte", nullable = false, length = 500)
    private String descripcion;

    @Column(name = "id_usuario_emisor", nullable = false)
    private Integer idUsuarioEmisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_reporte", nullable = false)
    private TipoReporte tipoReporte;

    @Column(name = "entidad_id")
    private Long entidadId; // ID de la oferta o cita reportada
}