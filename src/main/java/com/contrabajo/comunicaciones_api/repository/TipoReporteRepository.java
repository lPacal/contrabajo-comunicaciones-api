package com.contrabajo.comunicaciones_api.repository;

import com.contrabajo.comunicaciones_api.model.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional; // <-- IMPORTANTE IMPORTAR ESTO

@Repository
public interface TipoReporteRepository extends JpaRepository<TipoReporte, Integer> {
    
    // Con esta simple línea, Spring Boot crea automáticamente la consulta SQL:
    // SELECT * FROM tipo_reporte WHERE nombre = ?
    Optional<TipoReporte> findByNombre(String nombre);
    
}