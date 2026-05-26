package com.subastar.subastar.service;

import com.subastar.subastar.dto.chat.ConversacionResumen;
import com.subastar.subastar.dto.chat.MensajeChatResponse;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.ChatMensaje;
import com.subastar.subastar.model.Cliente;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatMensajeRepository chatMensajeRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;
    private final SeguroExtraRepository seguroExtraRepository;

    public List<ConversacionResumen> getConversaciones(String email) {
        Integer clienteId = getClienteId(email);
        List<ConversacionResumen> convs = new ArrayList<>();

        for (String tipo : List.of("soporte", "bot", "poliza")) {
            ConversacionResumen c = new ConversacionResumen();
            c.setTipo(tipo);
            c.setTitulo(switch (tipo) {
                case "soporte" -> "Soporte";
                case "bot" -> "Bot";
                default -> "Póliza de seguro";
            });

            chatMensajeRepository
                    .findTopByClienteIdentificadorAndTipoOrderByTimestampMsgDesc(clienteId, tipo)
                    .ifPresentOrElse(
                            m -> c.setSubtitulo(m.getContenido().length() > 50
                                    ? m.getContenido().substring(0, 50) + "..." : m.getContenido()),
                            () -> c.setSubtitulo("")
                    );

            int noLeidos = chatMensajeRepository
                    .countByClienteIdentificadorAndTipoAndLeidoFalse(clienteId, tipo);
            c.setMensajesNoLeidos(noLeidos);

            if ("poliza".equals(tipo)) {
                List<?> polizas = seguroExtraRepository.findByBeneficiarioId(clienteId);
                c.setPorcentajeCompletitud(polizas.isEmpty() ? 0 : 100);
            }
            convs.add(c);
        }
        return convs;
    }

    @Transactional
    public List<MensajeChatResponse> getMensajes(String email, String tipo) {
        Integer clienteId = getClienteId(email);

        List<ChatMensaje> mensajes = chatMensajeRepository
                .findByClienteIdentificadorAndTipoOrderByTimestampMsgAsc(clienteId, tipo);

        // Marcar como leídos
        mensajes.stream().filter(m -> !m.isLeido()).forEach(m -> {
            m.setLeido(true);
            chatMensajeRepository.save(m);
        });

        // Si no hay mensajes de bot, crear uno de bienvenida
        if (mensajes.isEmpty() && "bot".equals(tipo)) {
            Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
            String nombre = (cliente != null && cliente.getPersona() != null)
                    ? cliente.getPersona().getNombre() : "usuario";
            ChatMensaje bienvenida = new ChatMensaje();
            bienvenida.setCliente(cliente);
            bienvenida.setTipo("bot");
            bienvenida.setEmisor("bot");
            bienvenida.setContenido("¡Hola " + nombre + "! Bienvenido a SubastAR. Aquí recibirás notificaciones sobre tus subastas.");
            bienvenida.setLeido(true);
            chatMensajeRepository.save(bienvenida);
            mensajes = List.of(bienvenida);
        }

        return mensajes.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private MensajeChatResponse toResponse(ChatMensaje m) {
        MensajeChatResponse r = new MensajeChatResponse();
        r.setId(m.getId());
        r.setEmisor(m.getEmisor());
        r.setContenido(m.getContenido());
        r.setTimestamp(m.getTimestampMsg());
        r.setLeido(m.isLeido());
        return r;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
