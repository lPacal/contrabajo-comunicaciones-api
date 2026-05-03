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
}