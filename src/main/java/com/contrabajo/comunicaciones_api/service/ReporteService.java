package com.contrabajo.comunicaciones_api.service;

import com.contrabajo.comunicaciones_api.dto.*;
import com.contrabajo.comunicaciones_api.model.*;
import com.contrabajo.comunicaciones_api.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final TipoReporteRepository tipoReporteRepository;
    private final RestTemplate restTemplate;

    private final String MS_USUARIOS_URL = "http://localhost:8081/api/usuarios/";
    private final String MS_SERVICIOS_URL = "http://localhost:8082/api/ofertas/";

    public List<TipoReporte> obtenerTiposReporte() {
        return tipoReporteRepository.findAll();
    }

    @Transactional
    public ReporteResponseDTO crearReporte(Integer idUsuarioEmisor, CrearReporteDTO dto) {
        TipoReporte tipo = tipoReporteRepository.findById(dto.getIdTipoReporte())
                .orElseThrow(() -> new RuntimeException("Tipo de reporte inválido"));

        Reporte reporte = new Reporte();
        // USAMOS TU MODELO ORIGINAL INTACTO
        reporte.setIdUsuarioEmisor(idUsuarioEmisor); 
        reporte.setEntidadId(dto.getIdOfertaServicio()); 
        reporte.setTipoReporte(tipo);
        reporte.setDescripcion(dto.getComentario()); 
        reporte.setResuelto(false);

        return convertirADTO(reporteRepository.save(reporte));
    }

    public List<ReporteResponseDTO> listarReportes(String busqueda, Integer idTipoReporte, String estadoRevision, Boolean ordenarRecientes) {
        Sort sort = ordenarRecientes 
                ? Sort.by(Sort.Direction.DESC, "id") 
                : Sort.by(Sort.Direction.ASC, "id");
        
        List<Reporte> reportesBD = reporteRepository.findAll(sort);

        List<ReporteResponseDTO> listaDTOs = reportesBD.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());

        if (idTipoReporte != null) {
            listaDTOs = listaDTOs.stream()
                    .filter(r -> r.getIdTipoReporte().equals(idTipoReporte))
                    .collect(Collectors.toList());
        }

        if (estadoRevision != null && !estadoRevision.trim().isEmpty()) {
            listaDTOs = listaDTOs.stream()
                    .filter(r -> r.getEstadoRevision().equals(estadoRevision))
                    .collect(Collectors.toList());
        }

        if (busqueda != null && !busqueda.trim().isEmpty()) {
            String b = busqueda.toLowerCase();
            listaDTOs = listaDTOs.stream()
                    .filter(r -> 
                        r.getTipoReporteNombre().toLowerCase().contains(b) ||
                        r.getUsuarioReportadoUsername().toLowerCase().contains(b) ||
                        r.getServicioTitulo().toLowerCase().contains(b) ||
                        r.getComentario().toLowerCase().contains(b)
                    )
                    .collect(Collectors.toList());
        }

        return listaDTOs;
    }

    public ReporteResponseDTO obtenerDetalle(Long idReporte) {
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));
        return convertirADTO(reporte);
    }

    @Transactional
    public ReporteResponseDTO resolverReporte(Long idReporte, String medida) {
        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        if (reporte.getResuelto()) {
            throw new RuntimeException("El reporte ya está resuelto.");
        }

        reporte.setResolucionReporte(medida);
        reporte.setResuelto(true);

        return convertirADTO(reporteRepository.save(reporte));
    }

    // HELPER PARA LLEVARSE EL TOKEN DEL USUARIO A LOS OTROS MS
    private HttpEntity<String> crearAuthHeader() {
        HttpHeaders headers = new HttpHeaders();
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    headers.set("Authorization", authHeader);
                }
            }
        } catch (Exception ignored) {}
        return new HttpEntity<>(headers);
    }

    // API COMPOSITION: CONVIERTE LA BD AL JSON QUE ESPERA KOTLIN
    private ReporteResponseDTO convertirADTO(Reporte reporte) {
        ReporteResponseDTO dto = new ReporteResponseDTO();
        
        dto.setIdReporte(reporte.getId());
        dto.setIdEmisor(reporte.getIdUsuarioEmisor());
        dto.setIdOfertaServicio(reporte.getEntidadId());
        dto.setIdTipoReporte(reporte.getTipoReporte().getId());
        dto.setTipoReporteNombre(reporte.getTipoReporte().getNombre());
        dto.setComentario(reporte.getDescripcion());
        dto.setFechaCreacion(reporte.getFechaCreacion());
        dto.setMedidaAplicada(reporte.getResolucionReporte());
        dto.setEstadoRevision(reporte.getResuelto() ? "RESUELTO" : "PENDIENTE");

        // Datos que el frontend pide pero que no están en la BD, se devuelven nulos o genéricos
        dto.setIdChatCita(null);
        dto.setIdModeradorRevisor(null);
        dto.setFechaRevision(null);
        dto.setEmisorUsername("usuario_" + reporte.getIdUsuarioEmisor());
        dto.setUsuarioReportadoUsername("desconocido");
        dto.setUsuarioReportadoNombre("Usuario Desconocido");
        dto.setServicioTitulo("Servicio #" + reporte.getEntidadId());
        dto.setServicioFotoUrl("");

        // Llamamos a los otros MS con el token del usuario activo
        HttpEntity<String> entity = crearAuthHeader();

        try {
            ResponseEntity<Map> response = restTemplate.exchange(MS_USUARIOS_URL + reporte.getIdUsuarioEmisor(), HttpMethod.GET, entity, Map.class);
            if (response.getBody() != null) dto.setEmisorUsername((String) response.getBody().get("username"));
        } catch (Exception ignored) { }

        if (reporte.getEntidadId() != null) {
            try {
                ResponseEntity<Map> responseOferta = restTemplate.exchange(MS_SERVICIOS_URL + reporte.getEntidadId(), HttpMethod.GET, entity, Map.class);
                if (responseOferta.getBody() != null) {
                    dto.setServicioTitulo((String) responseOferta.getBody().get("titulo"));
                    dto.setServicioFotoUrl((String) responseOferta.getBody().get("fotoUrlReferencia"));
                    
                    Integer idTrabajador = (Integer) responseOferta.getBody().get("idTrabajador");
                    if (idTrabajador != null) {
                        dto.setIdUsuarioReportado(idTrabajador);
                        try {
                            ResponseEntity<Map> responseReportado = restTemplate.exchange(MS_USUARIOS_URL + idTrabajador, HttpMethod.GET, entity, Map.class);
                            if (responseReportado.getBody() != null) {
                                dto.setUsuarioReportadoUsername((String) responseReportado.getBody().get("username"));
                                String nombre = (String) responseReportado.getBody().get("nombre");
                                String apellido = (String) responseReportado.getBody().get("apellidoPaterno");
                                dto.setUsuarioReportadoNombre((nombre + " " + (apellido != null ? apellido : "")).trim());
                            }
                        } catch (Exception ignored) { }
                    }
                }
            } catch (Exception ignored) { }
        }

        return dto;
    }
}