package com.contrabajo.comunicaciones_api.service;

import com.contrabajo.comunicaciones_api.dto.*;
import com.contrabajo.comunicaciones_api.model.*;
import com.contrabajo.comunicaciones_api.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final TipoReporteRepository tipoReporteRepository;
    private final RestTemplate restTemplate;
    
    @Value("${MS_USUARIOS_URL:http://localhost:8081}/api/usuarios/")
    private String MS_USUARIOS_URL;

    @Value("${MS_SERVICIOS_URL:http://localhost:8082}/api/ofertas/")
    private String MS_SERVICIOS_URL;

    
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
        String medidaNormalizada = medida == null ? "" : medida.trim();
        Integer idUsuarioHint = extraerIdUsuarioHint(medidaNormalizada);
        String medidaBase = limpiarMedida(medidaNormalizada);

        Reporte reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> new RuntimeException("Reporte no encontrado"));

        if (reporte.getResuelto()) {
            return convertirADTO(reporte);
        }

        HttpEntity<String> entity = crearAuthHeader();
        try {
            if ("IGNORAR_REPORTE".equalsIgnoreCase(medidaBase)) {
                // No aplica acción externa, solo se marca como resuelto.
            } else if ("DESACTIVAR_SERVICIO".equalsIgnoreCase(medidaBase)) {
                Long idOfertaServicio = validarOfertaAsociada(reporte);
                try {
                    restTemplate.exchange(
                            MS_SERVICIOS_URL + idOfertaServicio + "/disponibilidad/desactivar",
                            HttpMethod.PATCH,
                            entity,
                            Object.class
                    );
                } catch (HttpStatusCodeException e) {
                    // Solo ignoramos 404: el servicio ya no existe (eliminado), se trata como estado final.
                    // Un 400 es un fallo real (ej: oferta ya borrada, rol insuficiente) y debe propagarse.
                    if (e.getStatusCode().value() != 404) {
                        throw e;
                    }
                }
            } else if ("ELIMINAR_SERVICIO".equalsIgnoreCase(medidaBase)) {
                Long idOfertaServicio = validarOfertaAsociada(reporte);
                restTemplate.exchange(
                        MS_SERVICIOS_URL + idOfertaServicio,
                        HttpMethod.DELETE,
                        entity,
                        Object.class
                );
            } else if (medidaBase.toUpperCase().startsWith("SUSPENDER_USUARIO_HASTA")) {
                Integer idUsuarioReportado = resolverTrabajadorReportado(reporte, entity, idUsuarioHint);
                // Formato: SUSPENDER_USUARIO_HASTA:{fechaFin}
                //      o:  SUSPENDER_USUARIO_HASTA:{fechaInicio}/{fechaFin}
                String parametro = extraerParametro(medidaBase);
                String fechaFin;
                String fechaInicio = null;
                if (parametro.contains("/")) {
                    String[] partes = parametro.split("/", 2);
                    fechaInicio = partes[0].trim();
                    fechaFin = partes[1].trim();
                } else {
                    fechaFin = parametro;
                }
                Map<String, Object> bodySuspension = new HashMap<>();
                bodySuspension.put("accion", "SUSPENDER_USUARIO_HASTA");
                bodySuspension.put("fechaFin", fechaFin);
                bodySuspension.put("motivo", "Reporte #" + reporte.getId());
                if (fechaInicio != null) bodySuspension.put("fechaInicio", fechaInicio);
                restTemplate.exchange(
                        MS_USUARIOS_URL + idUsuarioReportado + "/moderacion",
                        HttpMethod.PATCH,
                        new HttpEntity<>(bodySuspension, entity.getHeaders()),
                        Object.class
                );
            } else if ("BANEAR_USUARIO".equalsIgnoreCase(medidaBase)) {
                Integer idUsuarioReportado = resolverTrabajadorReportado(reporte, entity, idUsuarioHint);
                restTemplate.exchange(
                        MS_USUARIOS_URL + idUsuarioReportado + "/moderacion",
                        HttpMethod.PATCH,
                        new HttpEntity<>(Map.of(
                                "accion", "BANEAR_USUARIO",
                                "motivo", "Reporte #" + reporte.getId()
                        ), entity.getHeaders()),
                        Object.class
                );
            } else {
                throw new RuntimeException("Medida de moderación no soportada: " + medidaBase);
            }
        } catch (HttpStatusCodeException e) {
            String detalle = e.getResponseBodyAsString();
            throw new RuntimeException("No se pudo aplicar la medida (" + e.getStatusCode().value() + "): " + detalle);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo aplicar la medida: " + e.getMessage());
        }

        reporte.setResolucionReporte(medidaBase);
        reporte.setResuelto(true);

        return convertirADTO(reporteRepository.save(reporte));
    }

    private Long validarOfertaAsociada(Reporte reporte) {
        Long idOfertaServicio = reporte.getEntidadId();
        if (idOfertaServicio == null) {
            throw new RuntimeException("El reporte no tiene servicio asociado para aplicar moderación.");
        }
        return idOfertaServicio;
    }

    private Integer resolverTrabajadorReportado(Reporte reporte, HttpEntity<String> entity, Integer idUsuarioHint) {
        if (idUsuarioHint != null && idUsuarioHint > 0) {
            return idUsuarioHint;
        }
        Long idOfertaServicio = validarOfertaAsociada(reporte);
        ResponseEntity<Map> responseOferta = restTemplate.exchange(
                MS_SERVICIOS_URL + idOfertaServicio,
                HttpMethod.GET,
                entity,
                Map.class
        );
        if (responseOferta.getBody() == null || responseOferta.getBody().get("idTrabajador") == null) {
            throw new RuntimeException("No se pudo resolver el trabajador reportado.");
        }
        Object idTrabajador = responseOferta.getBody().get("idTrabajador");
        if (idTrabajador instanceof Integer value) return value;
        if (idTrabajador instanceof Number value) return value.intValue();
        throw new RuntimeException("idTrabajador inválido en oferta reportada.");
    }

    private String extraerParametro(String medida) {
        int idx = medida.indexOf(':');
        if (idx < 0 || idx + 1 >= medida.length()) {
            throw new RuntimeException("Falta la fecha fin de suspensión en la medida.");
        }
        return medida.substring(idx + 1).trim();
    }

    private Integer extraerIdUsuarioHint(String medida) {
        int idx = medida.toUpperCase().indexOf("|USR:");
        if (idx < 0) return null;
        String valor = medida.substring(idx + 5).trim();
        if (valor.isBlank()) return null;
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String limpiarMedida(String medida) {
        int idx = medida.toUpperCase().indexOf("|USR:");
        if (idx < 0) return medida;
        return medida.substring(0, idx).trim();
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
