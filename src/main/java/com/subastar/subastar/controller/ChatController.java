package com.subastar.subastar.controller;

import com.subastar.subastar.dto.chat.ConversacionResumen;
import com.subastar.subastar.dto.chat.MensajeChatResponse;
import com.subastar.subastar.dto.chat.EnviarMensajeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import com.subastar.subastar.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionResumen>> getConversaciones(
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(chatService.getConversaciones(user.getUsername()));
    }

    @GetMapping("/conversaciones/{tipo}")
    public ResponseEntity<List<MensajeChatResponse>> getMensajes(
            @PathVariable String tipo,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(chatService.getMensajes(user.getUsername(), tipo));
    }

    @PostMapping("/conversaciones/{tipo}/mensajes")
    public ResponseEntity<MensajeChatResponse> enviarMensaje(
            @PathVariable String tipo,
            @Valid @RequestBody EnviarMensajeRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.enviarMensaje(user.getUsername(), tipo, req));
    }
}
