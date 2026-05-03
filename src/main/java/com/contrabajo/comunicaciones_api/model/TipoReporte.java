package com.contrabajo.comunicaciones_api.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tipo_reporte")
@Data
public class TipoReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_reporte")
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;
}