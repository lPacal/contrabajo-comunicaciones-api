package com.contrabajo.comunicaciones_api.config;

import com.contrabajo.comunicaciones_api.model.TipoReporte;
import com.contrabajo.comunicaciones_api.repository.TipoReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TipoReporteRepository tipoReporteRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // 1. Catálogo completo de Tipos de Reporte
        List<String> tiposReporteBase = Arrays.asList(
                "Servicio falso",
                "Falta de profesionalidad",
                "Contenido inapropiado",
                "Cobro abusivo o enganoso",
                "Incumplimiento de horario",
                "Suplantacion o perfil sospechoso"
        );

        // Aseguramos que cada uno exista individualmente
        for (String nombre : tiposReporteBase) {
            asegurarTipoReporte(nombre);
        }

        System.out.println("--> Catálogo de Tipos de Reporte verificado y cargado.");
    }

    // Método auxiliar con la misma lógica que usó tu compañero
    private void asegurarTipoReporte(String nombre) {
        tipoReporteRepository.findByNombre(nombre)
                .orElseGet(() -> {
                    TipoReporte nuevoTipo = new TipoReporte();
                    nuevoTipo.setNombre(nombre);
                    return tipoReporteRepository.save(nuevoTipo);
                });
    }
}