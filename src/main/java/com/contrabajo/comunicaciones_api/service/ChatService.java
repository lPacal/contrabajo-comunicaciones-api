package com.contrabajo.comunicaciones_api.service;

import com.contrabajo.comunicaciones_api.dto.ChatResponseDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatRequestDTO;
import com.contrabajo.comunicaciones_api.dto.MensajeChatResponseDTO;
import com.contrabajo.comunicaciones_api.model.ChatOferta;
import com.contrabajo.comunicaciones_api.model.MensajeChat;
import com.contrabajo.comunicaciones_api.repository.ChatOfertaRepository;
import com.contrabajo.comunicaciones_api.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatOfertaRepository chatOfertaRepository;
    private final MensajeChatRepository mensajeChatRepository;
    private final ChatCryptoService chatCryptoService;
    
    // ¡La varita mágica de los WebSockets!
    private final SimpMessagingTemplate messagingTemplate; 

    // ==========================================
    // 1. INICIAR O RECUPERAR EL CHAT (El "Room")
    // ==========================================
    @Transactional
    public ChatOferta iniciarChat(Integer idTrabajador, Integer idCliente, Integer idOfertaServicio,
                                  String usernameTrabajador, String usernameCliente, String tituloServicio) {
        // Reusar solo chat ACTIVO. Si no hay activo, se crea uno nuevo siempre.
        var existente = chatOfertaRepository
                .findTopByIdTrabajadorAndIdClienteAndIdOfertaServicioAndActivoTrueOrderByIdDesc(
                        idTrabajador, idCliente, idOfertaServicio
                );

        if (existente.isPresent()) {
            ChatOferta chat = existente.get();
            // Retrocompatibilidad: si los campos de visualizacion estan vacios (chat antiguo),
            // los actualizamos con los valores recibidos ahora para que el header funcione.
            boolean actualizar = false;
            if (usernameTrabajador != null && !usernameTrabajador.isBlank()
                    && (chat.getUsernameTrabajador() == null || chat.getUsernameTrabajador().isBlank())) {
                chat.setUsernameTrabajador(usernameTrabajador);
                actualizar = true;
            }
            if (usernameCliente != null && !usernameCliente.isBlank()
                    && (chat.getUsernameCliente() == null || chat.getUsernameCliente().isBlank())) {
                chat.setUsernameCliente(usernameCliente);
                actualizar = true;
            }
            if (tituloServicio != null && !tituloServicio.isBlank()
                    && (chat.getTituloServicio() == null || chat.getTituloServicio().isBlank())) {
                chat.setTituloServicio(tituloServicio);
                actualizar = true;
            }
            return actualizar ? chatOfertaRepository.save(chat) : chat;
        }
        return crearNuevoChat(idTrabajador, idCliente, idOfertaServicio, usernameTrabajador, usernameCliente, tituloServicio);
    }

    private ChatOferta crearNuevoChat(Integer idTrabajador, Integer idCliente, Integer idOfertaServicio,
                                      String usernameTrabajador, String usernameCliente, String tituloServicio) {
        ChatOferta nuevoChat = new ChatOferta();
        nuevoChat.setIdTrabajador(idTrabajador);
        nuevoChat.setIdCliente(idCliente);
        nuevoChat.setIdOfertaServicio(idOfertaServicio);
        nuevoChat.setUsernameTrabajador(usernameTrabajador);
        nuevoChat.setUsernameCliente(usernameCliente);
        nuevoChat.setTituloServicio(tituloServicio);
        ChatOferta chatGuardado = chatOfertaRepository.save(nuevoChat);

        // Mensaje de sistema: avisar al trabajador que alguien esta interesado en su servicio
        String nombreCliente = (usernameCliente != null && !usernameCliente.isBlank())
                ? "@" + usernameCliente : "Un cliente";
        String nombreServicio = (tituloServicio != null && !tituloServicio.isBlank())
                ? "\"" + tituloServicio + "\"" : "tu servicio";
        String contenidoSistema = nombreCliente + " está interesado en " + nombreServicio + ".";

        MensajeChat msgSistema = new MensajeChat();
        msgSistema.setContenido(chatCryptoService.encryptForStorage(contenidoSistema));
        msgSistema.setIdEmisor(idCliente);    // el cliente es quien inicia el contacto
        msgSistema.setIdReceptor(idTrabajador);
        msgSistema.setTipo((byte) 1);         // tipo sistema
        msgSistema.setChatOferta(chatGuardado);

        MensajeChat msgGuardado = mensajeChatRepository.save(msgSistema);
        MensajeChatResponseDTO responseDTO = convertirADto(msgGuardado);

        // Emitir por WebSocket al trabajador para que aparezca en tiempo real
        String destino = "/topic/chat/" + idTrabajador;
        messagingTemplate.convertAndSend(destino, responseDTO);

        return chatGuardado;
    }

    // ==========================================
    // 2. LA MAGIA: ENVIAR MENSAJE Y DISPARAR WS
    // ==========================================
    @Transactional
    public MensajeChatResponseDTO enviarMensaje(MensajeChatRequestDTO dto, Integer idEmisor) {
        ChatOferta chat = chatOfertaRepository.findById(dto.getIdChatOferta())
                .orElseThrow(() -> new RuntimeException("El chat no existe."));

        // Seguridad: Validar que el emisor sea parte del chat
        if (!chat.getIdCliente().equals(idEmisor) && !chat.getIdTrabajador().equals(idEmisor)) {
            throw new RuntimeException("No tienes permiso para participar en este chat.");
        }
        if (!chat.getActivo()) {
            throw new RuntimeException("No puedes enviar mensajes. Este chat ha sido cerrado porque el servicio finalizó.");
        }

        // ==========================================
        //  FIX SEGURIDAD: Calcular el Receptor automáticamente
        // ==========================================
        Integer idReceptorCalculado;
        if (chat.getIdCliente().equals(idEmisor)) {
            idReceptorCalculado = chat.getIdTrabajador(); // Si soy el cliente, le hablo al trabajador
        } else {
            idReceptorCalculado = chat.getIdCliente();    // Si soy el trabajador, le hablo al cliente
        }

        // A. Guardar en Base de Datos
        MensajeChat mensaje = new MensajeChat();
        mensaje.setContenido(chatCryptoService.encryptForStorage(dto.getContenido()));
        mensaje.setIdEmisor(idEmisor);
        mensaje.setIdReceptor(idReceptorCalculado); // Usamos el calculado, 100% seguro
        byte tipoMensaje = (dto.getTipo() != null && dto.getTipo() == 1) ? (byte) 1 : (byte) 0;
        mensaje.setTipo(tipoMensaje);
        mensaje.setChatOferta(chat);
        
        MensajeChat guardado = mensajeChatRepository.save(mensaje);
        MensajeChatResponseDTO responseDTO = convertirADto(guardado);

        // B. Emitir por WebSocket al receptor real
        messagingTemplate.convertAndSend("/topic/chat/" + idReceptorCalculado, responseDTO);

        return responseDTO;
    }

    // ==========================================
    // 3. OBTENER HISTORIAL (El "GET" inicial)
    // ==========================================
    @Transactional(readOnly = true)
    public List<MensajeChatResponseDTO> obtenerHistorial(Long idChatOferta, Integer idUsuarioSolicitante) {
        ChatOferta chat = chatOfertaRepository.findById(idChatOferta)
                .orElseThrow(() -> new RuntimeException("El chat no existe."));

        // Seguridad: Solo los participantes pueden leer el historial
        if (!chat.getIdCliente().equals(idUsuarioSolicitante) && !chat.getIdTrabajador().equals(idUsuarioSolicitante)) {
            throw new RuntimeException("Acceso denegado al historial de este chat.");
        }

        return mensajeChatRepository.findByChatOfertaIdOrderByFechaEnvioAsc(idChatOferta)
                .stream()
                .map(this::convertirADto)
                .collect(Collectors.toList());
    }

    // Traductor Entidad -> DTO
    private MensajeChatResponseDTO convertirADto(MensajeChat mensaje) {
        MensajeChatResponseDTO dto = new MensajeChatResponseDTO();
        dto.setId(mensaje.getId());
        dto.setIdChatOferta(mensaje.getChatOferta().getId());
        dto.setIdEmisor(mensaje.getIdEmisor());
        dto.setIdReceptor(mensaje.getIdReceptor());
        dto.setContenido(chatCryptoService.decryptForRead(mensaje.getContenido()));
        dto.setFechaEnvio(mensaje.getFechaEnvio());
        dto.setFechaRecibido(mensaje.getFechaRecibido());
        dto.setFechaLeido(mensaje.getFechaLeido());
        dto.setTipo(mensaje.getTipo() != null ? mensaje.getTipo().intValue() : 0);
        return dto;
    }


    // ==========================================
    // 4. LISTAR CHATS DEL USUARIO AUTENTICADO
    // ==========================================
    @Transactional(readOnly = true)
    public List<ChatResponseDTO> listarChatsUsuario(Integer idUsuario) {
        List<ChatOferta> chats = chatOfertaRepository.findByIdTrabajadorOrIdCliente(idUsuario, idUsuario);
        return chats.stream()
                .map(chat -> toChatResponseDTO(chat, idUsuario))
                .sorted((a, b) -> {
                    if (a.getFechaUltimoMensaje() == null) return 1;
                    if (b.getFechaUltimoMensaje() == null) return -1;
                    return b.getFechaUltimoMensaje().compareTo(a.getFechaUltimoMensaje());
                })
                .collect(Collectors.toList());
    }

    // ==========================================
    // 5. VINCULAR CITA A UN CHAT
    // ==========================================
    @Transactional
    public ChatResponseDTO vincularCita(Long idChat, Integer idCita, Integer idUsuario) {
        ChatOferta chat = chatOfertaRepository.findById(idChat)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado."));
        if (!chat.getIdCliente().equals(idUsuario) && !chat.getIdTrabajador().equals(idUsuario)) {
            throw new RuntimeException("Acceso denegado: no participas en este chat.");
        }
        chat.setIdCita(idCita);
        ChatOferta actualizado = chatOfertaRepository.save(chat);
        return toChatResponseDTO(actualizado, idUsuario);
    }

    // Traductor ChatOferta -> ChatResponseDTO con preview y no leidos
    private ChatResponseDTO toChatResponseDTO(ChatOferta chat, Integer idUsuario) {
        ChatResponseDTO dto = new ChatResponseDTO();
        dto.setId(chat.getId());
        dto.setIdTrabajador(chat.getIdTrabajador());
        dto.setIdCliente(chat.getIdCliente());
        dto.setIdOfertaServicio(chat.getIdOfertaServicio());
        dto.setIdCita(chat.getIdCita());
        dto.setActivo(chat.getActivo());
        dto.setFechaCreacion(chat.getFechaCreacion());
        dto.setUsernameTrabajador(chat.getUsernameTrabajador());
        dto.setUsernameCliente(chat.getUsernameCliente());
        dto.setTituloServicio(chat.getTituloServicio());

        mensajeChatRepository.findTopByChatOfertaIdOrderByFechaEnvioDesc(chat.getId())
                .ifPresent(ultimo -> {
                    dto.setUltimoMensaje(chatCryptoService.decryptForRead(ultimo.getContenido()));
                    dto.setFechaUltimoMensaje(ultimo.getFechaEnvio());
                });

        long noLeidos = mensajeChatRepository
                .countByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(chat.getId(), idUsuario);
        dto.setMensajesNoLeidos(noLeidos);

        return dto;
    }

    @Transactional
    public void desactivarChatEspecifico(Integer idTrabajador, Integer idOfertaServicio, Integer idUsuarioAutenticado) {
        // Buscamos el chat que coincida con la Oferta y el Trabajador (sin asumir quién es el usuario aún)
        chatOfertaRepository.findByIdOfertaServicioAndIdTrabajador(idOfertaServicio, idTrabajador)
                .ifPresentOrElse(chat -> {

                    // Permitir cierre por cualquier participante del chat (cliente o trabajador).
                    if (!chat.getIdCliente().equals(idUsuarioAutenticado) && !chat.getIdTrabajador().equals(idUsuarioAutenticado)) {
                        throw new RuntimeException("No tienes permiso para modificar este chat.");
                    }

                    if (!Boolean.TRUE.equals(chat.getActivo())) {
                        return;
                    }

                    Integer idReceptor = chat.getIdCliente().equals(idUsuarioAutenticado)
                            ? chat.getIdTrabajador()
                            : chat.getIdCliente();

                    MensajeChat msgSistema = new MensajeChat();
                    msgSistema.setContenido(chatCryptoService.encryptForStorage("El chat fue finalizado."));
                    msgSistema.setIdEmisor(idUsuarioAutenticado);
                    msgSistema.setIdReceptor(idReceptor);
                    msgSistema.setTipo((byte) 1);
                    msgSistema.setChatOferta(chat);
                    mensajeChatRepository.save(msgSistema);

                    chat.setActivo(false);
                    chatOfertaRepository.save(chat);

                    emitirEventoChatCerrado(chat);
                }, () -> {
                    throw new RuntimeException("No se encontró un chat activo para esta combinación de servicio.");
                });
    }

    @Transactional
    public void desactivarChatPorId(Long idChat, Integer idUsuarioAutenticado) {
        ChatOferta chat = chatOfertaRepository.findById(idChat)
                .orElseThrow(() -> new RuntimeException("Chat no encontrado."));

        if (!chat.getIdCliente().equals(idUsuarioAutenticado) && !chat.getIdTrabajador().equals(idUsuarioAutenticado)) {
            throw new RuntimeException("No tienes permiso para modificar este chat.");
        }

        if (!Boolean.TRUE.equals(chat.getActivo())) {
            return;
        }

        Integer idReceptor = chat.getIdCliente().equals(idUsuarioAutenticado)
                ? chat.getIdTrabajador()
                : chat.getIdCliente();

        MensajeChat msgSistema = new MensajeChat();
        msgSistema.setContenido(chatCryptoService.encryptForStorage("El chat fue finalizado."));
        msgSistema.setIdEmisor(idUsuarioAutenticado);
        msgSistema.setIdReceptor(idReceptor);
        msgSistema.setTipo((byte) 1);
        msgSistema.setChatOferta(chat);
        mensajeChatRepository.save(msgSistema);

        chat.setActivo(false);
        chatOfertaRepository.save(chat);
        emitirEventoChatCerrado(chat);
    }

    private void emitirEventoChatCerrado(ChatOferta chat) {
        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "CHAT_CERRADO");
        evento.put("idChat", chat.getId());
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getIdCliente(), (Object) evento);
        messagingTemplate.convertAndSend("/topic/chat/" + chat.getIdTrabajador(), (Object) evento);
    }

    @Transactional
    public void marcarMensajesComoRecibidos(Long idChatOferta, Integer idUsuarioReceptor) {
        List<MensajeChat> mensajesPendientes = mensajeChatRepository
                .findByChatOfertaIdAndIdReceptorAndFechaRecibidoIsNull(idChatOferta, idUsuarioReceptor);

        if (!mensajesPendientes.isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            mensajesPendientes.forEach(m -> m.setFechaRecibido(ahora));
            mensajeChatRepository.saveAll(mensajesPendientes);

            // Avisar a cada emisor por WS que sus mensajes fueron entregados (tick gris)
            mensajesPendientes.stream()
                    .map(MensajeChat::getIdEmisor)
                    .distinct()
                    .forEach(idEmisor -> {
                        Map<String, Object> evento = new HashMap<>();
                        evento.put("tipo", "RECIBIDO");
                        evento.put("idChat", idChatOferta);
                        String destino = "/topic/chat/" + idEmisor;
                        messagingTemplate.convertAndSend(destino, (Object) evento);
                    });
        }
    }

    @Transactional
    public void marcarMensajesComoLeidos(Long idChatOferta, Integer idUsuarioReceptor) {
        List<MensajeChat> mensajesPendientes = mensajeChatRepository
                .findByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(idChatOferta, idUsuarioReceptor);

        if (!mensajesPendientes.isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            mensajesPendientes.forEach(m -> {
                // Si lo leyó, lógicamente también lo recibió (por si la app falló antes)
                if (m.getFechaRecibido() == null) {
                    m.setFechaRecibido(ahora);
                }
                m.setFechaLeido(ahora);
            });

            mensajeChatRepository.saveAll(mensajesPendientes);

            // Avisar a cada emisor por WS que sus mensajes fueron leídos (tick azul)
            mensajesPendientes.stream()
                    .map(MensajeChat::getIdEmisor)
                    .distinct()
                    .forEach(idEmisor -> {
                        Map<String, Object> evento = new HashMap<>();
                        evento.put("tipo", "LEIDO");
                        evento.put("idChat", idChatOferta);
                        String destino = "/topic/chat/" + idEmisor;
                        messagingTemplate.convertAndSend(destino, (Object) evento);
                    });
        }
    }

}
