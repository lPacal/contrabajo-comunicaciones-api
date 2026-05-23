package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.CrearReporteDTO;
import com.contrabajo.comunicaciones_api.dto.ReporteResponseDTO;
import com.contrabajo.comunicaciones_api.model.TipoReporte;
import com.contrabajo.comunicaciones_api.service.ReporteService;
import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReporteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReporteService reporteService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ReporteController reporteController;

    private ObjectMapper objectMapper;
    private CrearReporteDTO crearDTO;
    private ReporteResponseDTO reporteResponseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reporteController).build();
        objectMapper = new ObjectMapper();

        crearDTO = new CrearReporteDTO();
        crearDTO.setIdTipoReporte(1);
        crearDTO.setIdOfertaServicio(100L);
        crearDTO.setComentario("Comentario de prueba");

        reporteResponseDTO = new ReporteResponseDTO();
        reporteResponseDTO.setIdReporte(10L);
        reporteResponseDTO.setIdEmisor(5);
        reporteResponseDTO.setIdOfertaServicio(100L);
        reporteResponseDTO.setIdTipoReporte(1);
        reporteResponseDTO.setTipoReporteNombre("Conducta inapropiada");
        reporteResponseDTO.setComentario("Comentario de prueba");
        reporteResponseDTO.setEstadoRevision("PENDIENTE");
        reporteResponseDTO.setServicioTitulo("Servicio #100");
    }

    @Test
    void testObtenerTipos_Exitoso() throws Exception {
        TipoReporte tipoReporte = new TipoReporte();
        tipoReporte.setId(1);
        tipoReporte.setNombre("Conducta inapropiada");
        when(reporteService.obtenerTiposReporte()).thenReturn(List.of(tipoReporte));

        mockMvc.perform(get("/api/reportes/tipos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Conducta inapropiada"));
    }

    @Test
    void testCrearReporte_Exitoso() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(5);
        when(reporteService.crearReporte(eq(5), any(CrearReporteDTO.class))).thenReturn(reporteResponseDTO);

        mockMvc.perform(post("/api/reportes")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crearDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReporte").value(10))
                .andExpect(jsonPath("$.idEmisor").value(5))
                .andExpect(jsonPath("$.idOfertaServicio").value(100))
                .andExpect(jsonPath("$.tipoReporteNombre").value("Conducta inapropiada"))
                .andExpect(jsonPath("$.estadoRevision").value("PENDIENTE"));
    }

    @Test
    void testCrearReporte_Error() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(5);
        when(reporteService.crearReporte(eq(5), any(CrearReporteDTO.class)))
                .thenThrow(new RuntimeException("Tipo de reporte inválido"));

        mockMvc.perform(post("/api/reportes")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crearDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tipo de reporte inválido"));
    }

    @Test
    void testListarReportes_Exitoso() throws Exception {
        when(reporteService.listarReportes("conducta", 1, "PENDIENTE", false))
                .thenReturn(List.of(reporteResponseDTO));

        mockMvc.perform(get("/api/reportes")
                .param("busqueda", "conducta")
                .param("idTipoReporte", "1")
                .param("estadoRevision", "PENDIENTE")
                .param("ordenarRecientes", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idReporte").value(10))
                .andExpect(jsonPath("$[0].servicioTitulo").value("Servicio #100"));
    }

    @Test
    void testObtenerDetalle_Exitoso() throws Exception {
        when(reporteService.obtenerDetalle(10L)).thenReturn(reporteResponseDTO);

        mockMvc.perform(get("/api/reportes/{idReporte}", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idReporte").value(10))
                .andExpect(jsonPath("$.comentario").value("Comentario de prueba"));
    }

    @Test
    void testObtenerDetalle_Error() throws Exception {
        when(reporteService.obtenerDetalle(99L)).thenThrow(new RuntimeException("Reporte no encontrado"));

        mockMvc.perform(get("/api/reportes/{idReporte}", 99))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Reporte no encontrado"));
    }

    @Test
    void testRevisarReporte_Exitoso() throws Exception {
        reporteResponseDTO.setEstadoRevision("RESUELTO");
        reporteResponseDTO.setMedidaAplicada("Advertencia aplicada");
        when(reporteService.resolverReporte(10L, "Advertencia aplicada")).thenReturn(reporteResponseDTO);

        mockMvc.perform(patch("/api/reportes/{idReporte}/revision", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("medidaAplicada", "Advertencia aplicada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRevision").value("RESUELTO"))
                .andExpect(jsonPath("$.medidaAplicada").value("Advertencia aplicada"));
    }

    @Test
    void testRevisarReporte_MedidaObligatoria() throws Exception {
        mockMvc.perform(patch("/api/reportes/{idReporte}/revision", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("medidaAplicada", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La medida aplicada es obligatoria."));

        verify(reporteService, never()).resolverReporte(anyLong(), anyString());
    }
}
