package com.contrabajo.comunicaciones_api.repository;

import com.contrabajo.comunicaciones_api.model.MensajeChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MensajeChatRepository extends JpaRepository<MensajeChat, Long> {
    
    // El que ya teníamos para el historial
    List<MensajeChat> findByChatOfertaIdOrderByFechaEnvioAsc(Long idChatOferta);

    // NUEVO: Buscar mensajes donde yo soy el receptor y aún NO tienen fecha de recibido
    List<MensajeChat> findByChatOfertaIdAndIdReceptorAndFechaRecibidoIsNull(Long idChatOferta, Integer idReceptor);

    // NUEVO: Buscar mensajes donde yo soy el receptor y aún NO tienen fecha de leído
    List<MensajeChat> findByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(Long idChatOferta, Integer idReceptor);

    // Para preview de lista de chats: ultimo mensaje del chat
    java.util.Optional<MensajeChat> findTopByChatOfertaIdOrderByFechaEnvioDesc(Long idChatOferta);

    // Para preview de lista de chats: cantidad de mensajes no leidos para un receptor
    long countByChatOfertaIdAndIdReceptorAndFechaLeidoIsNull(Long idChatOferta, Integer idReceptor);
}