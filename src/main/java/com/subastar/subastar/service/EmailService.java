package com.subastar.subastar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void enviarCodigoVerificacion(String email, String codigo) {
        log.info("[EMAIL MOCK] Para: {} | Código de verificación: {}", email, codigo);
    }

    public void enviarNotificacionAprobacion(String email, String codigo) {
        log.info("[EMAIL MOCK] Para: {} | Tu registro fue aprobado. Código: {}", email, codigo);
    }
}
