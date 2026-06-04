package com.subastar.subastar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCodigoVerificacion(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Código de verificación - Subastar");
        message.setText("Tu código de verificación es: " + codigo);
        mailSender.send(message);
    }

    public void enviarNotificacionAprobacion(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Registro aprobado - Subastar");
        message.setText("Tu registro fue aprobado. Tu código es: " + codigo);
        mailSender.send(message);
    }

    public void enviarCodigoRecuperacionPassword(String email, String codigo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Recuperación de contraseña - SubastAR");
        message.setText(
                "Tu código para recuperar la contraseña es: " + codigo + "\n\n"
                + "Este código vence en 10 minutos. Si no solicitaste recuperar tu contraseña, ignorá este correo."
        );
        mailSender.send(message);
    }
}
