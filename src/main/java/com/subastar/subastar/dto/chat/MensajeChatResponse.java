package com.subastar.subastar.dto.chat;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MensajeChatResponse {
    private Integer id;
    private String emisor;
    private String contenido;
    private LocalDateTime timestamp;
    private boolean leido;
}
