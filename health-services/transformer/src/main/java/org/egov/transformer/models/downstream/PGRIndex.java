package org.egov.transformer.models.downstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.transformer.models.pgr.Service;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PGRIndex {
    @JsonProperty("service")
    private Service service;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("role")
    private String role;
    @JsonProperty("userAddress")
    private String userAddress;
    @JsonProperty("boundaryHierarchy")
    private ObjectNode boundaryHierarchy;
    @JsonProperty("taskDates")
    private String taskDates;

}
