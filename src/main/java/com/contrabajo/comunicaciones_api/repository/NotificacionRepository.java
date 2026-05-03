package com.contrabajo.comunicaciones_api.repository;

import com.contrabajo.comunicaciones_api.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    
    // Trae las notificaciones no leídas de un usuario, de la más nueva a la más antigua
    List<Notificacion> findByIdUsuarioReceptorAndLeidaFalseOrderByFechaCreacionDesc(Integer idUsuarioReceptor);
}