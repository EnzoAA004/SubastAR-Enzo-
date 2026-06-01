package com.subastar.subastar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "productos_detalle")
public class ProductoDetalle {
    @Id
    @Column(name = "producto_id")
    private Integer productoId;

    @Column(name = "cliente_id")
    private Integer clienteId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "cantidad_elementos")
    private Integer cantidadElementos;

    @Column(name = "epoca_origen")
    private String epocaOrigen;

    @Column(name = "artista_disenador")
    private String artistaDisenador;

    @Column(name = "fecha_creacion_obra")
    private LocalDate fechaCreacionObra;

    @Column(name = "datos_historicos")
    private String datosHistoricos;

    @Column(name = "informacion_adicional")
    private String informacionAdicional;

    @Column(name = "precio_base_sugerido", precision = 18, scale = 2)
    private BigDecimal precioBaseSugerido;

    @Column(name = "estado_solicitud", nullable = false)
    private String estadoSolicitud;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "ubicacion_deposito")
    private String ubicacionDeposito;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @PrePersist
    public void prePersist() {
        if (estadoSolicitud == null) estadoSolicitud = "en_revision";
    }
}
