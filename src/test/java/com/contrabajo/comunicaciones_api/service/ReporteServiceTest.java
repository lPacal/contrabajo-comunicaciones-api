package com.contrabajo.comunicaciones_api.service;

import com.contrabajo.comunicaciones_api.dto.CrearReporteDTO;
import com.contrabajo.comunicaciones_api.dto.ReporteResponseDTO;
import com.contrabajo.comunicaciones_api.model.Reporte;
import com.contrabajo.comunicaciones_api.model.TipoReporte;
import com.contrabajo.comunicaciones_api.repository.ReporteRepository;
import com.contrabajo.comunicaciones_api.repository.TipoReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @Mock
    private TipoReporteRepository tipoReporteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ReporteService reporteService;

    private TipoReporte tipoReporte;
    private Reporte reportePendiente;
    private CrearReporteDTO crearDTO;

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(reporteService, "MS_USUARIOS_URL", "http://localhost:8081/api/usuarios/");
        ReflectionTestUtils.setField(reporteService, "MS_SERVICIOS_URL", "http://localhost:8082/api/ofertas/");

        tipoReporte = new TipoReporte();
        tipoReporte.setId(1);
        tipoReporte.setNombre("Conducta inapropiada");

        reportePendiente = new Reporte();
        reportePendiente.setId(10L);
        reportePendiente.setIdUsuarioEmisor(5);
        reportePendiente.setEntidadId(100L);
        reportePendiente.setTipoReporte(tipoReporte);
        reportePendiente.setDescripcion("Comentario de prueba");
        reportePendiente.setResuelto(false);

        crearDTO = new CrearReporteDTO();
        crearDTO.setIdTipoReporte(1);
        crearDTO.setIdOfertaServicio(100L);
        crearDTO.setComentario("Comentario de prueba");
    }

    @Test
    void testObtenerTiposReporte_Exitoso() {
        when(tipoReporteRepository.findAll()).thenReturn(List.of(tipoReporte));

        List<TipoReporte> resultado = reporteService.obtenerTiposReporte();

        assertEquals(1, resultado.size());
        assertEquals("Conducta inapropiada", resultado.get(0).getNombre());
    }

    @Test
    void testCrearReporte_Exitoso() {
        when(tipoReporteRepository.findById(1)).thenReturn(Optional.of(tipoReporte));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> {
            Reporte reporte = invocation.getArgument(0);
            reporte.setId(10L);
            return reporte;
        });
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.crearReporte(5, crearDTO);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getIdReporte());
        assertEquals(5, resultado.getIdEmisor());
        assertEquals(100L, resultado.getIdOfertaServicio());
        assertEquals("Conducta inapropiada", resultado.getTipoReporteNombre());
        assertEquals("PENDIENTE", resultado.getEstadoRevision());
    }

    @Test
    void testCrearReporte_TipoInvalido() {
        when(tipoReporteRepository.findById(1)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reporteService.crearReporte(5, crearDTO));

        assertEquals("Tipo de reporte inválido", exception.getMessage());
    }

    @Test
    void testListarReportes_FiltraPorBusquedaYEstado() {
        Reporte otro = crearReporte(11L, "Otro comentario", false);
        when(reporteRepository.findAll(any(Sort.class))).thenReturn(List.of(reportePendiente, otro));
        stubRestTemplateDefaults();

        List<ReporteResponseDTO> resultado = reporteService.listarReportes("comentario de prueba", 1, "PENDIENTE", true);

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).getIdReporte());
        assertEquals("PENDIENTE", resultado.get(0).getEstadoRevision());
    }

    @Test
    void testObtenerDetalle_Encontrado() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.obtenerDetalle(10L);

        assertEquals(10L, resultado.getIdReporte());
        assertEquals("Comentario de prueba", resultado.getComentario());
    }

    @Test
    void testObtenerDetalle_NoEncontrado() {
        when(reporteRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reporteService.obtenerDetalle(99L));

        assertEquals("Reporte no encontrado", exception.getMessage());
    }

    @Test
    void testResolverReporte_IgnorarReporte() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "IGNORAR_REPORTE");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        assertEquals("IGNORAR_REPORTE", resultado.getMedidaAplicada());
        verify(restTemplate, never()).exchange(contains("/disponibilidad/desactivar"), eq(HttpMethod.PATCH), any(), eq(Object.class));
        verify(restTemplate, never()).exchange(contains("/api/ofertas/100"), eq(HttpMethod.DELETE), any(), eq(Object.class));
    }

    @Test
    void testResolverReporte_DesactivarServicio() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "DESACTIVAR_SERVICIO");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        verify(restTemplate, times(1)).exchange(contains("/api/ofertas/100/disponibilidad/desactivar"), eq(HttpMethod.PATCH), any(), eq(Object.class));
    }

    @Test
    void testResolverReporte_NoSoportado() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        stubRestTemplateDefaults();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reporteService.resolverReporte(10L, "CAMBIAR_COLOR"));

        assertTrue(exception.getMessage().contains("no soportada"));
    }

    @Test
    void testResolverReporte_EliminarServicio() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "ELIMINAR_SERVICIO");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        verify(restTemplate).exchange(contains("/api/ofertas/100"), eq(HttpMethod.DELETE), any(), eq(Object.class));
    }

    @Test
    void testResolverReporte_SuspenderUsuarioHasta_ConHint() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "SUSPENDER_USUARIO_HASTA:2026-12-31|USR:8");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        verify(restTemplate).exchange(contains("/usuarios/8/moderacion"), eq(HttpMethod.PATCH), any(), eq(Object.class));
    }

    @Test
    void testResolverReporte_BanearUsuario() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "BANEAR_USUARIO");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        verify(restTemplate).exchange(contains("/usuarios/8/moderacion"), eq(HttpMethod.PATCH), any(), eq(Object.class));
    }

    @Test
    void testObtenerDetalle_Enriquecido() {
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.obtenerDetalle(10L);

        assertEquals("usuario_mock", resultado.getEmisorUsername());
        assertEquals("Servicio mock", resultado.getServicioTitulo());
        assertEquals("usuario_mock", resultado.getUsuarioReportadoUsername());
        assertEquals(8, resultado.getIdUsuarioReportado());
    }

    @Test
    void testResolverReporte_YaResueltoRetornaDto() {
        reportePendiente.setResuelto(true);
        reportePendiente.setResolucionReporte("IGNORAR_REPORTE");
        when(reporteRepository.findById(10L)).thenReturn(Optional.of(reportePendiente));
        stubRestTemplateDefaults();

        ReporteResponseDTO resultado = reporteService.resolverReporte(10L, "IGNORAR_REPORTE");

        assertEquals("RESUELTO", resultado.getEstadoRevision());
        assertEquals("IGNORAR_REPORTE", resultado.getMedidaAplicada());
        verify(reporteRepository, never()).save(any());
    }

    private void stubRestTemplateDefaults() {
        Map<String, Object> usuario = Map.of(
                "username", "usuario_mock",
                "nombre", "Nombre",
                "apellidoPaterno", "Apellido"
        );
        Map<String, Object> oferta = Map.of(
                "titulo", "Servicio mock",
                "fotoUrlReferencia", "http://img/mock.png",
                "idTrabajador", 8
        );
        lenient().when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenAnswer(invocation -> {
                    String url = invocation.getArgument(0, String.class);
                    if (url.contains("/api/ofertas/")) {
                        return ResponseEntity.ok(oferta);
                    }
                    return ResponseEntity.ok(usuario);
                });
        lenient().when(restTemplate.exchange(anyString(), eq(HttpMethod.PATCH), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());
        lenient().when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok().build());
    }

    private Reporte crearReporte(Long id, String descripcion, boolean resuelto) {
        Reporte reporte = new Reporte();
        reporte.setId(id);
        reporte.setIdUsuarioEmisor(6);
        reporte.setEntidadId(101L);
        reporte.setTipoReporte(tipoReporte);
        reporte.setDescripcion(descripcion);
        reporte.setResuelto(resuelto);
        return reporte;
    }
}
