package org.egov.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
* HouseholdResponse
*/
@Validated
@javax.annotation.Generated(value = "org.egov.codegen.SpringBootCodegen", date = "2022-12-21T13:41:16.379+05:30")

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HouseholdResponse   {
        @JsonProperty("ResponseInfo")
      @NotNull

  @Valid


    private org.egov.common.contract.response.ResponseInfo responseInfo = null;

        @JsonProperty("Household")
    
  @Valid


    private List<Household> household = null;


        public HouseholdResponse addHouseholdItem(Household householdItem) {
            if (this.household == null) {
            this.household = new ArrayList<>();
            }
        this.household.add(householdItem);
        return this;
        }

}

