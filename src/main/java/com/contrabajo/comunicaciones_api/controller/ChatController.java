package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.ChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.ChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.VincularCitaDTO;
import com.contrabajo.comunicaciones_api.model.ChatOferta;
import com.contrabajo.comunicaciones_api.service.ChatService;
import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request; 

    @PostMapping("/iniciar")
    public ResponseEntity<?> iniciarChat(@Valid @RequestBody ChatRequestDTO dto) {
        try {
            Integer idCliente = obtenerIdDelToken();
            ChatOferta chat = chatService.iniciarChat(
                    dto.getIdTrabajador(), idCliente, dto.getIdOfertaServicio(),
                    dto.getUsernameTrabajador(), dto.getUsernameCliente(), dto.getTituloServicio());
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/mensaje")
    public ResponseEntity<?> enviarMensaje(@Valid @RequestBody MensajeChatRequestDTO dto) {
        try {
            // Sacamos quién envía el mensaje directo del Token (¡Seguridad al máximo!)
            Integer idEmisor = obtenerIdDelToken();
            MensajeChatResponseDTO enviado = chatService.enviarMensaje(dto, idEmisor);
            return ResponseEntity.ok(enviado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{idChat}/historial")
    public ResponseEntity<?> obtenerHistorial(@PathVariable Long idChat) {
        try {
            Integer idUsuario = obtenerIdDelToken();
            List<MensajeChatResponseDTO> historial = chatService.obtenerHistorial(idChat, idUsuario);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // UTILS: Extraer ID del JWT
    // ==========================================
    private Integer obtenerIdDelToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token de autorización no encontrado o inválido.");
        }
        return jwtUtil.extractId(authHeader.substring(7));
    }

    @PatchMapping("/desactivar")
    public ResponseEntity<?> desactivarChat(@Valid @RequestBody ChatRequestDTO dto) {
        try {
            // Le llamamos idUsuarioAutenticado porque puede ser el cliente o el trabajador intentando hacer la acción
            Integer idUsuarioAutenticado = obtenerIdDelToken();
            
            chatService.desactivarChatEspecifico(dto.getIdTrabajador(), dto.getIdOfertaServicio(), idUsuarioAutenticado);
            
            return ResponseEntity.ok("El chat ha sido finalizado y ya no permite nuevos mensajes.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{idChat}/desactivar")
    public ResponseEntity<?> desactivarChatPorId(@PathVariable Long idChat) {
        try {
            Integer idUsuarioAutenticado = obtenerIdDelToken();
            chatService.desactivarChatPorId(idChat, idUsuarioAutenticado);
            return ResponseEntity.ok("El chat ha sido finalizado y ya no permite nuevos mensajes.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{idChat}/recibidos")
    public ResponseEntity<?> marcarComoRecibidos(@PathVariable Long idChat) {
        try {
            Integer idUsuario = obtenerIdDelToken();
            chatService.marcarMensajesComoRecibidos(idChat, idUsuario);
            return ResponseEntity.ok("Mensajes marcados como recibidos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{idChat}/leidos")
    public ResponseEntity<?> marcarComoLeidos(@PathVariable Long idChat) {
        try {
            Integer idUsuario = obtenerIdDelToken();
            chatService.marcarMensajesComoLeidos(idChat, idUsuario);
            return ResponseEntity.ok("Mensajes marcados como leídos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // GET /api/chats — lista de chats del usuario
    // ==========================================
    @GetMapping
    public ResponseEntity<?> listarMisChats() {
        try {
            Integer idUsuario = obtenerIdDelToken();
            List<ChatResponseDTO> chats = chatService.listarChatsUsuario(idUsuario);
            return ResponseEntity.ok(chats);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // PATCH /api/chats/{idChat}/vincular-cita — vincula una cita al chat
    // ==========================================
    @PatchMapping("/{idChat}/vincular-cita")
    public ResponseEntity<?> vincularCita(@PathVariable Long idChat, @RequestBody VincularCitaDTO dto) {
        try {
            Integer idUsuario = obtenerIdDelToken();
            ChatResponseDTO resultado = chatService.vincularCita(idChat, dto.getIdCita(), idUsuario);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
