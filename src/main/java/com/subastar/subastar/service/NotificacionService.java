package com.subastar.subastar.service;

import com.subastar.subastar.model.ChatMensaje;
import com.subastar.subastar.model.Cliente;
import com.subastar.subastar.repository.ChatMensajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificacionService {

    private final ChatMensajeRepository chatMensajeRepository;

    public void notificarGanadorSubasta(Cliente cliente, String nombreItem,
                                        BigDecimal importePujado, BigDecimal comision) {
        BigDecimal totalComision = comision != null ? comision : BigDecimal.ZERO;
        BigDecimal total = importePujado.add(totalComision);
        String contenido = "¡Ganaste el ítem \"" + nombreItem + "\"!\n"
                + "Importe ofertado: $" + importePujado + "\n"
                + "Comisión: $" + totalComision + "\n"
                + "Total a pagar: $" + total + "\n"
                + "El costo de envío a tu dirección declarada se calculará al momento de procesar la entrega.\n"
                + "Podés regularizar tu pago desde 'Mis compras'.";
        enviar(cliente, "compra", contenido);
    }

    public void notificarPagoRegularizado(Cliente cliente, String nombreItem, BigDecimal total) {
        String contenido = "Tu pago por el ítem \"" + nombreItem + "\" fue registrado correctamente.\n"
                + "Total abonado: $" + total;
        enviar(cliente, "bot", contenido);
    }

    public void notificarPujaRegistrada(Cliente cliente, String nombreItem, BigDecimal monto) {
        String contenido = "Tu puja de $" + monto + " por el ítem \"" + nombreItem + "\" fue registrada exitosamente.";
        enviar(cliente, "bot", contenido);
    }

    public void notificarMedioPagoAgregado(Cliente cliente, String descripcion) {
        String contenido = "Medio de pago agregado: " + descripcion + ".\nEstá pendiente de verificación por la empresa.";
        enviar(cliente, "bot", contenido);
    }

    public void notificarMedioPagoEliminado(Cliente cliente, String descripcion) {
        String contenido = "Medio de pago eliminado: " + descripcion + ".";
        enviar(cliente, "bot", contenido);
    }

    public void notificarBienConfirmado(Cliente cliente, String nombreBien) {
        String contenido = "Tu solicitud de consignación del bien \"" + nombreBien + "\" fue enviada para revisión.\n"
                + "Te notificaremos cuando la empresa complete la inspección.";
        enviar(cliente, "bot", contenido);
    }

    public void notificarMulta(Cliente cliente, BigDecimal monto, String motivo) {
        String contenido = "Se te aplicó una multa de $" + monto + ".\n"
                + "Motivo: " + motivo + "\n"
                + "Debés regularizarla antes de participar en otra subasta.";
        enviar(cliente, "multa", contenido);
    }

    public void notificarDevolucion(Cliente cliente, String nombreBien, BigDecimal costoDevolucion) {
        String contenido = "Rechazaste las condiciones para el bien \"" + nombreBien + "\".\n"
                + "El bien será devuelto a tu dirección con un costo de devolución de $" + costoDevolucion + ".\n"
                + "Deberás abonar este monto para recibir tu bien.";
        enviar(cliente, "bien", contenido);
    }

    public void notificarBienAceptado(Cliente cliente, String nombreBien, BigDecimal precioBase) {
        String contenido = "Tu bien \"" + nombreBien + "\" fue aceptado para subasta.\n"
                + "Precio base: $" + precioBase + "\n"
                + "Revisá los detalles en 'Mis bienes'.";
        enviar(cliente, "bien", contenido);
    }

    public void notificarBienRechazado(Cliente cliente, String nombreBien, String motivo) {
        String contenido = "Tu bien \"" + nombreBien + "\" no fue aceptado para subasta.\n"
                + "Motivo: " + (motivo != null ? motivo : "Sin especificar") + "\n"
                + "Podés contactar con la empresa para más información.";
        enviar(cliente, "bien", contenido);
    }

    private void enviar(Cliente cliente, String tipo, String contenido) {
        ChatMensaje msg = new ChatMensaje();
        msg.setCliente(cliente);
        msg.setTipo(tipo);
        msg.setEmisor("sistema");
        msg.setContenido(contenido);
        msg.setLeido(false);
        chatMensajeRepository.save(msg);
    }
}
