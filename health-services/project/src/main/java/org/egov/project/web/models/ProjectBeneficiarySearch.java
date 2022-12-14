package org.egov.project.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Size;

/**
* Search model for project beneficiary.
*/
    @ApiModel(description = "Search model for project beneficiary.")
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2022-12-02T17:32:25.406+05:30")

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectBeneficiarySearch   {

    @JsonProperty("id")
    private String id = null;

    @JsonProperty("tenantId")
    private String tenantId = null;

    @JsonProperty("projectId")
    @Size(min=2,max=64)
    private String projectId = null;

    @JsonProperty("beneficiaryId")
    @Size(min=2,max=64)
    private String beneficiaryId = null;

    @JsonProperty("dateOfRegistration")
    private Integer dateOfRegistration = null;

}

