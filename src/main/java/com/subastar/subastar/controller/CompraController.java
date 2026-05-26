package com.subastar.subastar.controller;

import com.subastar.subastar.dto.compra.CompraDetalle;
import com.subastar.subastar.dto.compra.CompraResumen;
import com.subastar.subastar.dto.compra.RegularizarPagoRequest;
import com.subastar.subastar.service.CompraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;

    @GetMapping
    public ResponseEntity<List<CompraResumen>> listar(
            @RequestParam(required = false) String estado_pago,
            @RequestParam(required = false) String estado_entrega,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.listar(user.getUsername(), estado_pago, estado_entrega));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraDetalle> getDetalle(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.getDetalle(user.getUsername(), id));
    }

    @PostMapping("/{id}/regularizar-pago")
    public ResponseEntity<CompraDetalle> regularizarPago(
            @PathVariable Integer id,
            @Valid @RequestBody RegularizarPagoRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(compraService.regularizarPago(user.getUsername(), id, req));
    }

    @GetMapping("/{id}/factura")
    public ResponseEntity<Map<String, String>> getFactura(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        compraService.getDetalle(user.getUsername(), id);
        return ResponseEntity.ok(Map.of(
                "message", "Factura disponible",
                "url", "/api/v1/compras/" + id + "/factura/download"
        ));
    }

    @GetMapping("/{id}/factura/download")
    public ResponseEntity<byte[]> downloadFactura(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails user) {
        CompraDetalle detalle = compraService.getDetalle(user.getUsername(), id);
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String contenido = "FACTURA - SUBASTAR\n"
                + "==================\n"
                + "Fecha: " + fecha + "\n"
                + "Compra ID: " + id + "\n"
                + "Item: " + detalle.getNombreItem() + "\n"
                + "Subasta: " + detalle.getSubasta() + "\n"
                + "Valor pujado: " + detalle.getValorPujado() + "\n"
                + "Total: " + detalle.getTotal() + "\n"
                + "Estado de pago: " + detalle.getEstadoPago() + "\n";
        byte[] bytes = contenido.getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }
}
