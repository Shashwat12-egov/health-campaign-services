package org.egov.transformer.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Component
public class TransformerProperties {

    @Value("${transformer.producer.bulk.project.task.index.v1.topic}")
    private String transformerProducerBulkProjectTaskIndexV1Topic;

    @Value("${transformer.producer.bulk.project.staff.index.v1.topic}")
    private String transformerProducerBulkProjectStaffIndexV1Topic;

    @Value("${transformer.producer.service.task.index.v1.topic}")
    private String transformerProducerServiceTaskIndexV1Topic;

    @Value("${transformer.producer.bulk.project.index.v1.topic}")
    private String transformerProducerBulkProjectIndexV1Topic;

    @Value("${transformer.producer.bulk.stock.index.v1.topic}")
    private String transformerProducerBulkStockIndexV1Topic;

    @Value("${transformer.producer.household.member.index.v1.topic}")
    private String transformerProducerHouseholdMemberIndexV1Topic;

    @Value("${transformer.producer.bulk.household.index.v1.topic}")
    private String transformerProducerBulkHouseholdIndexV1Topic;

    @Value("${transformer.producer.side.effect.index.v1.topic}")
    private String transformerProducerSideEffectIndexV1Topic;

    @Value("${transformer.producer.service.index.v2.topic}")
    private String transformerProducerServiceIndexV2Topic;

    @Value("${transformer.producer.create.pgr.topic}")
    private String transformerProducerCreatePgrTopic;

    @Value("${transformer.producer.referral.index.v1.topic}")
    private String transformerProducerReferralIndexV1Topic;

    @Value("${transformer.producer.attendance.log.index.v1.topic}")
    private String transformerProducerAttendanceLogIndexV1Topic;

    @Value("${transformer.producer.attendance.register.index.v1.topic}")
    private String transformerProducerAttendanceRegisterIndexV1Topic;

    @Value("${egov.project.host}")
    private String projectHost;

    @Value("${egov.attendance.host}")
    private String attendanceHost;

    @Value("${egov.search.attendance.register.url}")
    private String attendanceRegisterSearchUrl;

    @Value("${egov.product.host}")
    private String productHost;

    @Value("${egov.search.project.url}")
    private String projectSearchUrl;

    @Value("${egov.search.project.task.url}")
    private String projectTaskSearchUrl;

    @Value("${egov.search.project.staff.url}")
    private String projectStaffSearchUrl;

    @Value("${egov.search.product.variant.url}")
    private String productVariantSearchUrl;

    @Value("${egov.search.project.beneficiary.url}")
    private String projectBeneficiarySearchUrl;

    @Value("${egov.household.host}")
    private String householdHost;

    @Value("${egov.search.household.url}")
    private String householdSearchUrl;

    @Value("${egov.individual.host}")
    private String individualHost;

    @Value("${egov.search.individual.url}")
    private String individualSearchUrl;

    @Value("${egov.location.host}")
    private String locationHost;

    @Value("${egov.location.endpoint}")
    private String locationSearchUrl;

    @Value("${egov.facility.host}")
    private String facilityHost;

    @Value("${egov.search.facility.url}")
    private String facilitySearchUrl;

    @Value("${egov.search.servicedefinition.url}")
    private String serviceDefinitionSearchUrl;

    @Value("${egov.servicedefinition.host}")
    private String serviceDefinitionHost;

    @Value("${search.api.limit:100}")
    private String searchApiLimit;

    @Value("${project.mdms.module}")
    private String mdmsModule;

    @Value("${transformer.localizations.mdms.module}")
    private String transformerLocalizationsMdmsModule;

    @Value("${project.staff.role.mdms.module}")
    private String projectStaffRolesMdmsModule;

    @Value("${boundary.label.name.province}")
    private String province;

    @Value("${boundary.label.name.locality}")
    private String locality;

    @Value("${boundary.label.name.district}")
    private String district;

    @Value("${boundary.label.name.village}")
    private String Village;

    @Value("${boundary.label.name.healthFacility}")
    private String healthFacility;

    @Value("${boundary.label.name.administrativeProvince}")
    private String administrativeProvince;

    @Value("${egov.program.mandate.limit}")
    private Integer programMandateLimit;

    @Value("${egov.program.mandate.dividing.factor}")
    private Double programMandateDividingFactor;

    @Value("${egov.program.mandate.comment}")
    private String programMandateComment;

    @Value("${egov.timestamp.timeZone}")
    private String timeZone;

    @Value("${referral.service.checklist.name}")
    private String checkListName;


}
