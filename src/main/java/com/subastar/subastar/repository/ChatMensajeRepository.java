package com.subastar.subastar.repository;

import com.subastar.subastar.model.ChatMensaje;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatMensajeRepository extends JpaRepository<ChatMensaje, Integer> {
    List<ChatMensaje> findByClienteIdentificadorAndTipoOrderByTimestampMsgAsc(Integer clienteId, String tipo);
    List<ChatMensaje> findByClienteIdentificadorAndTipoInOrderByTimestampMsgAsc(Integer clienteId, Collection<String> tipos);
    List<ChatMensaje> findByClienteIdentificadorAndTipoInOrderByTimestampMsgDesc(Integer clienteId, Collection<String> tipos);
    List<ChatMensaje> findByClienteIdentificadorAndTipoInAndLeidoFalse(Integer clienteId, Collection<String> tipos);
    Optional<ChatMensaje> findTopByClienteIdentificadorAndTipoOrderByTimestampMsgDesc(Integer clienteId, String tipo);
    int countByClienteIdentificadorAndTipoAndLeidoFalse(Integer clienteId, String tipo);
}
