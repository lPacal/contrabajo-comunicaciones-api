package com.contrabajo.comunicaciones_api.repository;

import com.contrabajo.comunicaciones_api.model.ChatOferta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatOfertaRepository extends JpaRepository<ChatOferta, Long> {
    
    // Busca si ya existe un chat exacto para esa oferta entre ese cliente y trabajador
    Optional<ChatOferta> findByIdTrabajadorAndIdClienteAndIdOfertaServicio(Integer idTrabajador, Integer idCliente, Integer idOfertaServicio);
    
    // Lista todos los chats donde el usuario participa (ya sea como cliente o trabajador)
    List<ChatOferta> findByIdTrabajadorOrIdCliente(Integer idTrabajador, Integer idCliente);

    Optional<ChatOferta> findByIdOfertaServicioAndIdTrabajador(Integer idOfertaServicio, Integer idTrabajador);
}