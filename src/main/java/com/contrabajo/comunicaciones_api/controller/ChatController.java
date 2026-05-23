package com.contrabajo.comunicaciones_api.controller;

import com.contrabajo.comunicaciones_api.dto.ChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.ChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.VincularCitaDTO;
import com.contrabajo.comunicaciones_api.model.ChatOferta;
import com.contrabajo.comunicaciones_api.service.ChatService;
import com.contrabajo.comunicaciones_api.utils.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "1. Chats", description = "Endpoints para iniciar chats, enviar mensajes y sincronizar estados de lectura")
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final JwtUtil jwtUtil;
    private final HttpServletRequest request; 

    @PostMapping("/iniciar")
    @Operation(
            summary = "Iniciar o recuperar chat",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Crea un nuevo chat entre el cliente autenticado y el trabajador de una oferta, o recupera el chat activo mas reciente si ya existe. " +
                    "Tambien guarda metadatos de visualizacion para evitar consultas cruzadas posteriores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat creado o recuperado correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatOferta.class))),
            @ApiResponse(responseCode = "400", description = "Error de negocio o token ausente.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Token de autorización no encontrado o inválido."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Enviar mensaje de chat",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Envía un mensaje al chat indicado. El emisor se obtiene desde el token y el receptor se calcula automaticamente para evitar suplantacion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensaje enviado y publicado por WebSocket.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensajeChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Chat inexistente, cerrado o usuario fuera del chat.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "No tienes permiso para participar en este chat."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Obtener historial de chat",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Devuelve los mensajes del chat en orden cronologico. Solo los participantes del chat pueden consultar el historial."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial devuelto correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MensajeChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Chat inexistente o acceso denegado al historial.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Acceso denegado al historial de este chat."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Desactivar chat por trabajador y oferta",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Finaliza el chat activo asociado a una combinacion de trabajador y oferta de servicio. El usuario autenticado debe ser participante del chat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat desactivado correctamente.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(example = "El chat ha sido finalizado y ya no permite nuevos mensajes."))),
            @ApiResponse(responseCode = "400", description = "No existe chat activo o el usuario no participa.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "No tienes permiso para modificar este chat."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Desactivar chat por ID",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Finaliza un chat por su ID y emite el evento WebSocket de cierre a ambos participantes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chat desactivado correctamente.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(example = "El chat ha sido finalizado y ya no permite nuevos mensajes."))),
            @ApiResponse(responseCode = "400", description = "Chat inexistente o usuario sin permisos.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Chat no encontrado."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Marcar mensajes como recibidos",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Marca como recibidos todos los mensajes pendientes donde el usuario autenticado es receptor."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensajes marcados como recibidos.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(example = "Mensajes marcados como recibidos."))),
            @ApiResponse(responseCode = "400", description = "Error al marcar mensajes como recibidos.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Token de autorización no encontrado o inválido."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Marcar mensajes como leidos",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Marca como leidos todos los mensajes pendientes donde el usuario autenticado es receptor. Si no estaban recibidos, tambien registra fecha de recibido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mensajes marcados como leidos.",
                    content = @Content(mediaType = "text/plain", schema = @Schema(example = "Mensajes marcados como leídos."))),
            @ApiResponse(responseCode = "400", description = "Error al marcar mensajes como leidos.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Token de autorización no encontrado o inválido."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Listar mis chats",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Lista todos los chats donde participa el usuario autenticado, incluyendo ultimo mensaje, contador de no leidos y metadatos de visualizacion."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de chats devuelta correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Token ausente o error de negocio.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Token de autorización no encontrado o inválido."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
    @Operation(
            summary = "Vincular cita a chat",
            description = "**Requiere Token JWT valido (BearerAuth)**<br><br>" +
                    "Asocia una cita creada en servicios-api con un chat existente. Solo los participantes del chat pueden vincular la cita."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cita vinculada correctamente.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Chat inexistente, cita invalida o usuario sin permisos.",
                    content = @Content(mediaType = "text/plain", examples = @ExampleObject(value = "Acceso denegado: no participas en este chat."))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente, invalido o expirado.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Rol no autorizado por la configuracion de seguridad.", content = @Content)
    })
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
