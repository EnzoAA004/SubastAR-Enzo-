package com.subastar.subastar.controller;

import com.subastar.subastar.service.AdminSubastaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/subastas")
@RequiredArgsConstructor
public class AdminSubastaController {

    private final AdminSubastaService adminSubastaService;

    @PostMapping("/{id}/abrir")
    public ResponseEntity<Map<String, String>> abrirSubasta(@PathVariable Integer id) {
        adminSubastaService.abrirSubasta(id);
        return ResponseEntity.ok(Map.of("message", "Subasta abierta correctamente"));
    }

    @PostMapping("/{id}/item-actual")
    public ResponseEntity<Map<String, String>> setItemActual(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> body) {
        Integer itemId = body.get("item_id");
        if (itemId == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Se requiere item_id"));
        }
        adminSubastaService.setItemActual(id, itemId);
        return ResponseEntity.ok(Map.of("message", "Ítem activo actualizado"));
    }

    @PostMapping("/{id}/cerrar-item")
    public ResponseEntity<Map<String, String>> cerrarItem(@PathVariable Integer id) {
        adminSubastaService.cerrarItem(id);
        return ResponseEntity.ok(Map.of("message", "Ítem cerrado. Ganador declarado y compra registrada si hubo pujas."));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Map<String, String>> cerrarSubasta(@PathVariable Integer id) {
        adminSubastaService.cerrarSubasta(id);
        return ResponseEntity.ok(Map.of("message", "Subasta cerrada correctamente"));
    }
}
