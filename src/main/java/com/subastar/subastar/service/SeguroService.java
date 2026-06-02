package com.subastar.subastar.service;

import com.subastar.subastar.dto.seguro.AumentarPolizaRequest;
import com.subastar.subastar.dto.seguro.PolizaResponse;
import com.subastar.subastar.exception.ForbiddenException;
import com.subastar.subastar.exception.ResourceNotFoundException;
import com.subastar.subastar.model.*;
import com.subastar.subastar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class SeguroService {

    private final SeguroRepository seguroRepository;
    private final SeguroExtraRepository seguroExtraRepository;
    private final ProductoRepository productoRepository;
    private final CredencialRepository credencialRepository;
    private final ClienteRepository clienteRepository;

    public List<PolizaResponse> listarMisPolizas(String email) {
        Integer clienteId = getClienteId(email);
        return seguroExtraRepository.findByBeneficiarioId(clienteId).stream()
                .map(extra -> seguroRepository.findById(extra.getPolizaId())
                        .map(seguro -> toPolizaResponse(seguro, extra, clienteId))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    public PolizaResponse getPoliza(String email, String polizaId) {
        Integer clienteId = getClienteId(email);
        Seguro seguro = seguroRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada"));
        SeguroExtra extra = seguroExtraRepository.findById(polizaId).orElse(null);

        if (extra != null && extra.getBeneficiarioId() != null && !extra.getBeneficiarioId().equals(clienteId)) {
            throw new ForbiddenException("La póliza no pertenece al usuario");
        }

        return toPolizaResponse(seguro, extra, clienteId);
    }

    @Transactional
    public PolizaResponse ampliarPoliza(String email, String polizaId, AumentarPolizaRequest req) {
        Integer clienteId = getClienteId(email);
        Seguro seguro = seguroRepository.findById(polizaId)
                .orElseThrow(() -> new ResourceNotFoundException("Póliza no encontrada"));
        SeguroExtra extra = seguroExtraRepository.findById(polizaId).orElse(null);

        if (extra == null || extra.getBeneficiarioId() == null || !extra.getBeneficiarioId().equals(clienteId)) {
            throw new ForbiddenException("La póliza no pertenece al usuario");
        }

        if (req.getNuevoValorAsegurado().compareTo(seguro.getImporte()) <= 0) {
            throw new com.subastar.subastar.exception.BadRequestException(
                    "El nuevo valor asegurado debe ser mayor al actual (" + seguro.getImporte() + ")");
        }

        seguro.setImporte(req.getNuevoValorAsegurado());
        seguroRepository.save(seguro);
        return toPolizaResponse(seguro, extra, clienteId);
    }

    private PolizaResponse toPolizaResponse(Seguro seguro, SeguroExtra extra, Integer clienteId) {
        PolizaResponse r = new PolizaResponse();
        r.setNumeroPoliza(seguro.getNroPoliza());
        r.setAseguradora(seguro.getCompania());
        r.setValorAsegurado(seguro.getImporte());

        if (extra != null) {
            r.setVigenciaDesde(extra.getVigenciaDesde());
            r.setVigenciaHasta(extra.getVigenciaHasta());
            r.setCobertura(extra.getCobertura());

            if (extra.getBeneficiarioId() != null) {
                clienteRepository.findById(extra.getBeneficiarioId()).ifPresent(c -> {
                    Persona p = c.getPersona();
                    if (p != null) r.setBeneficiario(p.getNombre());
                });
            }

            PolizaResponse.ContactoAseguradora contacto = new PolizaResponse.ContactoAseguradora();
            contacto.setTelefono(extra.getContactoTelefono());
            contacto.setEmail(extra.getContactoEmail());
            contacto.setWeb(extra.getContactoWeb());
            r.setContactoAseguradora(contacto);
        }

        List<String> piezas = productoRepository.findAll().stream()
                .filter(p -> seguro.getNroPoliza().equals(p.getSeguroNroPoliza()))
                .map(p -> p.getDescripcionCatalogo() != null ? p.getDescripcionCatalogo() : "Producto #" + p.getIdentificador())
                .collect(java.util.stream.Collectors.toList());
        r.setPiezas(piezas);

        return r;
    }

    private Integer getClienteId(String email) {
        return credencialRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"))
                .getPersonaId();
    }
}
