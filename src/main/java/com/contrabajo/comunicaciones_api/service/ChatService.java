package com.contrabajo.comunicaciones_api.service;

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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatOfertaRepository chatOfertaRepository;
    private final MensajeChatRepository mensajeChatRepository;
    
    // ¡La varita mágica de los WebSockets!
    private final SimpMessagingTemplate messagingTemplate; 

    // ==========================================
    // 1. INICIAR O RECUPERAR EL CHAT (El "Room")
    // ==========================================
    @Transactional
    public ChatOferta iniciarChat(Integer idTrabajador, Integer idCliente, Integer idOfertaServicio) {
        // Buscamos si ya existe, si no, lo creamos.
        return chatOfertaRepository.findByIdTrabajadorAndIdClienteAndIdOfertaServicio(idTrabajador, idCliente, idOfertaServicio)
                .orElseGet(() -> {
                    ChatOferta nuevoChat = new ChatOferta();
                    nuevoChat.setIdTrabajador(idTrabajador);
                    nuevoChat.setIdCliente(idCliente);
                    nuevoChat.setIdOfertaServicio(idOfertaServicio);
                    return chatOfertaRepository.save(nuevoChat);
                });
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
        mensaje.setContenido(dto.getContenido());
        mensaje.setIdEmisor(idEmisor);
        mensaje.setIdReceptor(idReceptorCalculado); // Usamos el calculado, 100% seguro
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
        dto.setContenido(mensaje.getContenido());
        dto.setFechaEnvio(mensaje.getFechaEnvio());
        dto.setFechaRecibido(mensaje.getFechaRecibido());
        dto.setFechaLeido(mensaje.getFechaLeido());
        return dto;
    }


    @Transactional
    public void desactivarChatEspecifico(Integer idTrabajador, Integer idOfertaServicio, Integer idUsuarioAutenticado) {
        // Buscamos el chat que coincida con la Oferta y el Trabajador (sin asumir quién es el usuario aún)
        chatOfertaRepository.findByIdOfertaServicioAndIdTrabajador(idOfertaServicio, idTrabajador)
                .ifPresentOrElse(chat -> {
                    
                    // 1. Validamos si el que intenta apagarlo es el Trabajador
                    if (chat.getIdTrabajador().equals(idUsuarioAutenticado)) {
                        throw new RuntimeException("Los trabajadores no tienen permiso para finalizar un chat. Solo el cliente puede realizar esta acción.");
                    }
                    
                    // 2. Validamos si el que intenta apagarlo NO es el Cliente (Seguridad extra)
                    if (!chat.getIdCliente().equals(idUsuarioAutenticado)) {
                        throw new RuntimeException("No tienes permiso para modificar este chat.");
                    }
                    
                    // 3. Si es el Cliente, procedemos a desactivarlo
                    chat.setActivo(false);
                    chatOfertaRepository.save(chat);
                    
                }, () -> {
                    throw new RuntimeException("No se encontró un chat activo para esta combinación de servicio.");
                });
    }

    @Transactional
    public void marcarMensajesComoRecibidos(Long idChatOferta, Integer idUsuarioReceptor) {
        List<MensajeChat> mensajesPendientes = mensajeChatRepository
                .findByChatOfertaIdAndIdReceptorAndFechaRecibidoIsNull(idChatOferta, idUsuarioReceptor);

        if (!mensajesPendientes.isEmpty()) {
            LocalDateTime ahora = LocalDateTime.now();
            mensajesPendientes.forEach(m -> m.setFechaRecibido(ahora));
            mensajeChatRepository.saveAll(mensajesPendientes);
            
            // Opcional: Avisar al emisor por WS que sus mensajes fueron entregados
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
        }
    }

}