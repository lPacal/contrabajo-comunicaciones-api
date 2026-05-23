package com.contrabajo.comunicaciones_api.service;

import com.contrabajo.comunicaciones_api.dto.ChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatResponseDTO;
import com.contrabajo.comunicaciones_api.model.ChatOferta;
import com.contrabajo.comunicaciones_api.model.MensajeChat;
import com.contrabajo.comunicaciones_api.repository.ChatOfertaRepository;
import com.contrabajo.comunicaciones_api.repository.MensajeChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatOfertaRepository chatOfertaRepository;

    @Mock
    private MensajeChatRepository mensajeChatRepository;

    @Mock
    private ChatCryptoService chatCryptoService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    private ChatOferta chatActivo;
    private MensajeChatRequestDTO mensajeDTO;

    @BeforeEach
    void setUp() {
        chatActivo = new ChatOferta();
        chatActivo.setId(10L);
        chatActivo.setIdTrabajador(2);
        chatActivo.setIdCliente(1);
        chatActivo.setIdOfertaServicio(50);
        chatActivo.setActivo(true);
        chatActivo.setUsernameTrabajador("trabajador_2");
        chatActivo.setUsernameCliente("cliente_1");
        chatActivo.setTituloServicio("Servicio de prueba");

        mensajeDTO = new MensajeChatRequestDTO();
        mensajeDTO.setIdChatOferta(10L);
        mensajeDTO.setContenido("Hola, necesito coordinar el servicio.");
    }

    @Test
    void testIniciarChat_ExistenteActivo() {
        when(chatOfertaRepository.findTopByIdTrabajadorAndIdClienteAndIdOfertaServicioAndActivoTrueOrderByIdDesc(2, 1, 50))
                .thenReturn(Optional.of(chatActivo));

        ChatOferta resultado = chatService.iniciarChat(2, 1, 50, "trabajador_2", "cliente_1", "Servicio de prueba");

        assertSame(chatActivo, resultado);
        verify(chatOfertaRepository, never()).save(any(ChatOferta.class));
    }

    @Test
    void testIniciarChat_NuevoConMensajeSistema() {
        when(chatOfertaRepository.findTopByIdTrabajadorAndIdClienteAndIdOfertaServicioAndActivoTrueOrderByIdDesc(2, 1, 50))
                .thenReturn(Optional.empty());
        when(chatCryptoService.encryptForStorage("@" + "cliente_1" + " está interesado en " + "\"" + "Servicio de prueba" + "\"."))
                .thenReturn("ENC_MSG");
        when(chatOfertaRepository.save(any(ChatOferta.class))).thenAnswer(invocation -> {
            ChatOferta chat = invocation.getArgument(0);
            chat.setId(10L);
            return chat;
        });
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatCryptoService.decryptForRead("ENC_MSG")).thenReturn("@" + "cliente_1" + " está interesado en " + "\"" + "Servicio de prueba" + "\".");

        ChatOferta resultado = chatService.iniciarChat(2, 1, 50, "trabajador_2", "cliente_1", "Servicio de prueba");

        assertEquals(10L, resultado.getId());
        assertEquals(2, resultado.getIdTrabajador());
        assertEquals(1, resultado.getIdCliente());
        assertEquals("trabajador_2", resultado.getUsernameTrabajador());
        assertEquals("cliente_1", resultado.getUsernameCliente());
        assertEquals("Servicio de prueba", resultado.getTituloServicio());
        verify(mensajeChatRepository, times(1)).save(any(MensajeChat.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/2"), any(MensajeChatResponseDTO.class));
    }

    @Test
    void testEnviarMensaje_Exitoso() {
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));
        when(chatCryptoService.encryptForStorage(mensajeDTO.getContenido())).thenReturn("ENC_MSG");
        when(chatCryptoService.decryptForRead("ENC_MSG")).thenReturn(mensajeDTO.getContenido());
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> {
            MensajeChat mensaje = invocation.getArgument(0);
            mensaje.setId(100L);
            return mensaje;
        });

        MensajeChatResponseDTO resultado = chatService.enviarMensaje(mensajeDTO, 1);

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
        assertEquals(10L, resultado.getIdChatOferta());
        assertEquals(1, resultado.getIdEmisor());
        assertEquals(2, resultado.getIdReceptor());
        assertEquals(mensajeDTO.getContenido(), resultado.getContenido());
        assertEquals(0, resultado.getTipo());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat/2"), any(MensajeChatResponseDTO.class));
    }

    @Test
    void testEnviarMensaje_SistemaAsignaTipoUno() {
        mensajeDTO.setTipo(1);
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));
        when(chatCryptoService.encryptForStorage(mensajeDTO.getContenido())).thenReturn("ENC_MSG");
        when(chatCryptoService.decryptForRead("ENC_MSG")).thenReturn(mensajeDTO.getContenido());
        when(mensajeChatRepository.save(any(MensajeChat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MensajeChatResponseDTO resultado = chatService.enviarMensaje(mensajeDTO, 1);

        assertEquals(1, resultado.getTipo());
    }

    @Test
    void testEnviarMensaje_ChatNoExiste() {
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatService.enviarMensaje(mensajeDTO, 1));

        assertEquals("El chat no existe.", exception.getMessage());
    }

    @Test
    void testObtenerHistorial_Exitoso() {
        MensajeChat mensaje = crearMensaje(100L, 1, 2, "ENC_MSG");
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));
        when(mensajeChatRepository.findByChatOfertaIdOrderByFechaEnvioAsc(10L)).thenReturn(List.of(mensaje));
        when(chatCryptoService.decryptForRead("ENC_MSG")).thenReturn("Mensaje inicial");

        List<MensajeChatResponseDTO> resultado = chatService.obtenerHistorial(10L, 2);

        assertEquals(1, resultado.size());
        assertEquals("Mensaje inicial", resultado.get(0).getContenido());
        assertEquals(0, resultado.get(0).getTipo());
    }

    @Test
    void testObtenerHistorial_AccesoDenegado() {
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> chatService.obtenerHistorial(10L, 99));

        assertEquals("Acceso denegado al historial de este chat.", exception.getMessage());
        verify(mensajeChatRepository, never()).findByChatOfertaIdOrderByFechaEnvioAsc(any());
    }

    @Test
    void testListarChatsUsuario_Exitoso() {
        ChatOferta chat = new ChatOferta();
        chat.setId(10L);
        chat.setIdTrabajador(2);
        chat.setIdCliente(1);
        chat.setIdOfertaServicio(50);
        chat.setActivo(true);
        chat.setUsernameTrabajador("trabajador_2");
        chat.setUsernameCliente("cliente_1");
        chat.setTituloServicio("Servicio de prueba");

        MensajeChat ultimo = crearMensaje(101L, 1, 2, "ENC_MSG");
        ultimo.setFechaEnvio(LocalDateTime.now());

        when(chatOfertaRepository.findByIdTrabajadorOrIdCliente(1, 1)).thenReturn(List.of(chat));
        when(mensajeChatRepository.findTopByChatOfertaIdOrderByFechaEnvioDesc(10L)).thenReturn(Optional.of(ultimo));
        when(chatCryptoService.decryptForRead("ENC_MSG")).thenReturn("Último mensaje");
        when(mensajeChatRepository.countByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(10L, 1)).thenReturn(3L);

        List<ChatResponseDTO> resultado = chatService.listarChatsUsuario(1);

        assertEquals(1, resultado.size());
        assertEquals("Último mensaje", resultado.get(0).getUltimoMensaje());
        assertEquals(3L, resultado.get(0).getMensajesNoLeidos());
    }

    @Test
    void testVincularCita_Exitoso() {
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));
        when(chatOfertaRepository.save(any(ChatOferta.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mensajeChatRepository.findTopByChatOfertaIdOrderByFechaEnvioDesc(10L)).thenReturn(Optional.empty());
        when(mensajeChatRepository.countByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(10L, 1)).thenReturn(0L);

        var resultado = chatService.vincularCita(10L, 77, 1);

        assertNotNull(resultado);
        assertEquals(77, resultado.getIdCita());
    }

    @Test
    void testDesactivarChatPorId_Exitoso() {
        when(chatOfertaRepository.findById(10L)).thenReturn(Optional.of(chatActivo));

        chatService.desactivarChatPorId(10L, 1);

        assertFalse(chatActivo.getActivo());
        verify(chatOfertaRepository, times(1)).save(chatActivo);
        verify(mensajeChatRepository, times(1)).save(any(MensajeChat.class));
    }

    @Test
    void testMarcarMensajesComoRecibidos_ActualizaPendientes() {
        MensajeChat mensaje = crearMensaje(100L, 1, 2, "ENC_MSG");
        when(mensajeChatRepository.findByChatOfertaIdAndIdReceptorAndFechaRecibidoIsNull(10L, 2))
                .thenReturn(List.of(mensaje));

        chatService.marcarMensajesComoRecibidos(10L, 2);

        assertNotNull(mensaje.getFechaRecibido());
        verify(mensajeChatRepository, times(1)).saveAll(List.of(mensaje));
    }

    @Test
    void testMarcarMensajesComoLeidos_ActualizaRecibidoYLeido() {
        MensajeChat mensaje = crearMensaje(100L, 1, 2, "ENC_MSG");
        when(mensajeChatRepository.findByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(10L, 2))
                .thenReturn(List.of(mensaje));

        chatService.marcarMensajesComoLeidos(10L, 2);

        assertNotNull(mensaje.getFechaRecibido());
        assertNotNull(mensaje.getFechaLeido());
        verify(mensajeChatRepository, times(1)).saveAll(List.of(mensaje));
    }

    private MensajeChat crearMensaje(Long id, Integer idEmisor, Integer idReceptor, String contenido) {
        MensajeChat mensaje = new MensajeChat();
        mensaje.setId(id);
        mensaje.setChatOferta(chatActivo);
        mensaje.setIdEmisor(idEmisor);
        mensaje.setIdReceptor(idReceptor);
        mensaje.setContenido(contenido);
        mensaje.setTipo((byte) 0);
        return mensaje;
    }
}
