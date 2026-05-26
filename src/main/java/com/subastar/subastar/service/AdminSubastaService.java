package com.subastar.subastar.service;

import com.subastar.subastar.exception.BadRequestException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminSubastaService {

    private final SubastaRepository subastaRepository;
    private final SubastaExtraRepository subastaExtraRepository;
    private final ItemCatalogoRepository itemCatalogoRepository;
    private final PujoRepository pujoRepository;
    private final RegistroDeSubastaRepository registroDeSubastaRepository;
    private final CompraExtraRepository compraExtraRepository;
    private final NotificacionService notificacionService;

    public void abrirSubasta(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        subasta.setEstado("abierta");
        subastaRepository.save(subasta);
    }

    public void setItemActual(Integer subastaId, Integer itemId) {
        SubastaExtra extra = subastaExtraRepository.findBySubastaId(subastaId)
                .orElseThrow(() -> new BadRequestException("Subasta sin configuración extra"));
        itemCatalogoRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));
        extra.setItemActualId(itemId);
        subastaExtraRepository.save(extra);
    }

    public void cerrarItem(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        SubastaExtra extra = subastaExtraRepository.findBySubastaId(subastaId)
                .orElseThrow(() -> new BadRequestException("Subasta sin configuración extra"));

        if (extra.getItemActualId() == null) {
            throw new BadRequestException("No hay ítem activo para cerrar");
        }

        ItemCatalogo item = itemCatalogoRepository.findById(extra.getItemActualId())
                .orElseThrow(() -> new ResourceNotFoundException("Ítem no encontrado"));

        pujoRepository.findTopByItemIdentificadorOrderByImporteDesc(item.getIdentificador())
                .ifPresent(pujoGanador -> {
                    pujoGanador.setGanador("si");
                    pujoRepository.save(pujoGanador);

                    Cliente ganador = pujoGanador.getAsistente().getCliente();
                    BigDecimal comision = item.getComision() != null ? item.getComision() : BigDecimal.ZERO;

                    RegistroDeSubasta registro = new RegistroDeSubasta();
                    registro.setSubasta(subasta);
                    registro.setProducto(item.getProducto());
                    registro.setCliente(ganador);
                    registro.setDuenio(item.getProducto().getDuenio());
                    registro.setImporte(pujoGanador.getImporte());
                    registro.setComision(comision);
                    registro = registroDeSubastaRepository.save(registro);

                    CompraExtra compraExtra = new CompraExtra();
                    compraExtra.setRegistroId(registro.getIdentificador());
                    compraExtraRepository.save(compraExtra);

                    // M-10: notificar al ganador por chat interno
                    String nombreItem = item.getProducto().getDescripcionCatalogo() != null
                            ? item.getProducto().getDescripcionCatalogo()
                            : "Ítem #" + item.getIdentificador();
                    notificacionService.notificarGanadorSubasta(ganador, nombreItem, pujoGanador.getImporte(), comision);
                });

        item.setSubastado("si");
        itemCatalogoRepository.save(item);

        extra.setItemActualId(null);
        subastaExtraRepository.save(extra);
    }

    public void cerrarSubasta(Integer subastaId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        subasta.setEstado("cerrada");
        subastaRepository.save(subasta);
    }
}
