package com.contrabajo.comunicaciones_api.repository;

import com.contrabajo.comunicaciones_api.model.Reporte;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {
    // Filtros dinámicos para el Moderador
    List<Reporte> findByResuelto(Boolean resuelto, Sort sort);
    List<Reporte> findByTipoReporteId(Integer idTipoReporte, Sort sort);
}