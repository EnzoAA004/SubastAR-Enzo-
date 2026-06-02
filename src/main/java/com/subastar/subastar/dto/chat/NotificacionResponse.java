package com.subastar.subastar.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificacionResponse {
    private Integer id;
    private String tipo;
    private String titulo;
    private String contenido;
    private LocalDateTime timestamp;
    private boolean leido;
}
