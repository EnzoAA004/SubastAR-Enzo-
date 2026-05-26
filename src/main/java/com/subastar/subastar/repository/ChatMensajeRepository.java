package com.subastar.subastar.repository;

import com.subastar.subastar.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Integer> {
    List<ChatMensaje> findByClienteIdentificadorAndTipoOrderByTimestampMsgAsc(Integer clienteId, String tipo);
    Optional<ChatMensaje> findTopByClienteIdentificadorAndTipoOrderByTimestampMsgDesc(Integer clienteId, String tipo);
    int countByClienteIdentificadorAndTipoAndLeidoFalse(Integer clienteId, String tipo);
}
