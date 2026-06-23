package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.ChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.ChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.VincularCitaDTO;
import com.contrabajo.comunicaciones_api.model.ChatOferta;
import com.contrabajo.comunicaciones_api.service.ChatService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ChatController chatController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testIniciarChat_Exitoso() throws Exception {
        ChatRequestDTO dto = new ChatRequestDTO();
        dto.setIdTrabajador(2);
        dto.setIdOfertaServicio(50);
        dto.setUsernameTrabajador("trabajador_2");
        dto.setUsernameCliente("cliente_1");
        dto.setTituloServicio("Servicio de prueba");

        ChatOferta chat = new ChatOferta();
        chat.setId(10L);
        chat.setIdTrabajador(2);
        chat.setIdCliente(1);
        chat.setIdOfertaServicio(50);
        chat.setUsernameTrabajador("trabajador_2");
        chat.setUsernameCliente("cliente_1");
        chat.setTituloServicio("Servicio de prueba");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);
        when(chatService.iniciarChat(2, 1, 50, "trabajador_2", "cliente_1", "Servicio de prueba"))
                .thenReturn(chat);

        mockMvc.perform(post("/api/chats/iniciar")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.idTrabajador").value(2))
                .andExpect(jsonPath("$.idCliente").value(1))
                .andExpect(jsonPath("$.tituloServicio").value("Servicio de prueba"));
    }

    @Test
    void testEnviarMensaje_Exitoso() throws Exception {
        MensajeChatRequestDTO dto = new MensajeChatRequestDTO();
        dto.setIdChatOferta(10L);
        dto.setContenido("Hola");
        dto.setTipo(0);

        MensajeChatResponseDTO responseDTO = new MensajeChatResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setIdChatOferta(10L);
        responseDTO.setIdEmisor(1);
        responseDTO.setIdReceptor(2);
        responseDTO.setContenido("Hola");
        responseDTO.setTipo(0);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);
        when(chatService.enviarMensaje(any(MensajeChatRequestDTO.class), eq(1))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/chats/mensaje")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.contenido").value("Hola"))
                .andExpect(jsonPath("$.tipo").value(0));
    }

    @Test
    void testObtenerHistorial_Exitoso() throws Exception {
        MensajeChatResponseDTO responseDTO = new MensajeChatResponseDTO();
        responseDTO.setId(100L);
        responseDTO.setIdChatOferta(10L);
        responseDTO.setContenido("Mensaje");
        responseDTO.setTipo(0);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);
        when(chatService.obtenerHistorial(10L, 1)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get("/api/chats/{idChat}/historial", 10)
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].contenido").value("Mensaje"));
    }

    @Test
    void testDesactivarChat_Exitoso() throws Exception {
        ChatRequestDTO dto = new ChatRequestDTO();
        dto.setIdTrabajador(2);
        dto.setIdOfertaServicio(50);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);

        mockMvc.perform(patch("/api/chats/desactivar")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("El chat ha sido finalizado y ya no permite nuevos mensajes."));

        verify(chatService).desactivarChatEspecifico(2, 50, 1);
    }

    @Test
    void testDesactivarChatPorId_Exitoso() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);

        mockMvc.perform(patch("/api/chats/{idChat}/desactivar", 10)
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("El chat ha sido finalizado y ya no permite nuevos mensajes."));

        verify(chatService).desactivarChatPorId(10L, 1);
    }

    @Test
    void testListarMisChats_Exitoso() throws Exception {
        ChatResponseDTO chat = new ChatResponseDTO();
        chat.setId(10L);
        chat.setTituloServicio("Servicio de prueba");

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);
        when(chatService.listarChatsUsuario(1)).thenReturn(List.of(chat));

        mockMvc.perform(get("/api/chats")
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].tituloServicio").value("Servicio de prueba"));
    }

    @Test
    void testVincularCita_Exitoso() throws Exception {
        VincularCitaDTO dto = new VincularCitaDTO();
        dto.setIdCita(77);

        ChatResponseDTO chat = new ChatResponseDTO();
        chat.setId(10L);
        chat.setIdCita(77);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);
        when(chatService.vincularCita(10L, 77, 1)).thenReturn(chat);

        mockMvc.perform(patch("/api/chats/{idChat}/vincular-cita", 10)
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCita").value(77));
    }

    @Test
    void testMarcarComoRecibidos_Exitoso() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);

        mockMvc.perform(patch("/api/chats/{idChat}/recibidos", 10)
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mensajes marcados como recibidos."));

        verify(chatService).marcarMensajesComoRecibidos(10L, 1);
    }

    @Test
    void testMarcarComoLeidos_Exitoso() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid_token");
        when(jwtUtil.extractId("valid_token")).thenReturn(1);

        mockMvc.perform(patch("/api/chats/{idChat}/leidos", 10)
                .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mensajes marcados como leídos."));

        verify(chatService).marcarMensajesComoLeidos(10L, 1);
    }

    @Test
    void testTokenInvalidoDevuelveBadRequest() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        mockMvc.perform(get("/api/chats/10/historial"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Token de autorización no encontrado o inválido."));
    }
}
