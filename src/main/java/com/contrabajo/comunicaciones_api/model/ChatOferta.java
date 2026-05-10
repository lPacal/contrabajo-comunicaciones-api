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

    @Column(name = "id_cita")
    private Integer idCita;

    @Column(nullable = false)
    private Boolean activo = true;

    // Datos de visualizacion desnormalizados — se guardan al crear el chat para evitar
    // joins cross-microservicio al listar chats.
    @Column(name = "username_trabajador", length = 100)
    private String usernameTrabajador;

    @Column(name = "username_cliente", length = 100)
    private String usernameCliente;

    @Column(name = "titulo_servicio", length = 200)
    private String tituloServicio;
}
