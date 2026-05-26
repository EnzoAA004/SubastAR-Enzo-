package com.subastar.subastar.dto.bien;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BienDetalle extends BienResumen {
    @JsonProperty("descripcion_tecnica")
    private String descripcionTecnica;
    @JsonProperty("cantidad_elementos")
    private Integer cantidadElementos;
    @JsonProperty("informacion_adicional")
    private String informacionAdicional;
    @JsonProperty("fotos_cargadas")
    private Integer fotosCargadas;
    @JsonProperty("documentacion_adjunta")
    private boolean documentacionAdjunta;
}
