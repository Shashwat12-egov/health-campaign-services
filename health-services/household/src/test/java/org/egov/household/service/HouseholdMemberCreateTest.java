package org.egov.household.service;

import org.egov.common.http.client.ServiceRequestClient;
import org.egov.common.producer.Producer;
import org.egov.common.validator.Validator;
import org.egov.household.config.HouseholdMemberConfiguration;
import org.egov.household.helper.HouseholdMemberRequestTestBuilder;
import org.egov.household.household.member.validators.HouseholdHeadValidator;
import org.egov.household.household.member.validators.IndividualValidator;
import org.egov.household.household.member.validators.IsDeletedValidator;
import org.egov.household.household.member.validators.NonExistentEntityValidator;
import org.egov.household.household.member.validators.NullIdValidator;
import org.egov.household.household.member.validators.RowVersionValidator;
import org.egov.household.household.member.validators.UniqueEntityValidator;
import org.egov.household.repository.HouseholdMemberRepository;
import org.egov.household.web.models.Household;
import org.egov.household.web.models.HouseholdMember;
import org.egov.household.web.models.HouseholdMemberBulkRequest;
import org.egov.household.web.models.HouseholdMemberRequest;
import org.egov.household.web.models.Individual;
import org.egov.household.web.models.IndividualResponse;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdMemberCreateTest {

    @InjectMocks
    HouseholdMemberService householdMemberService;

    @Mock
    HouseholdMemberRepository householdMemberRepository;

    @Mock
    HouseholdMemberConfiguration householdMemberConfiguration;

    @Mock
    HouseholdService householdService;

    @Mock
    private ServiceRequestClient serviceRequestClient;

    @Mock
    private NullIdValidator nullIdValidator;

    @Mock
    private NonExistentEntityValidator nonExistentEntityValidator;


    @Mock
    private UniqueEntityValidator uniqueEntityValidator;

    @Mock
    private IsDeletedValidator isDeletedValidator;

    @Mock
    private RowVersionValidator rowVersionValidator;

    @Mock
    private IndividualValidator individualValidator;

    @Mock
    private HouseholdHeadValidator householdHeadValidator;

    @Mock
    private HouseholdMemberEnrichmentService householdMemberEnrichmentService;

    private List<Validator<HouseholdMemberBulkRequest, HouseholdMember>> validators;

    @BeforeEach
    void setUp() throws Exception {
        List<String> idList = new ArrayList<>();
        idList.add("some-id");
        validators = Arrays.asList(
                nullIdValidator,
                nonExistentEntityValidator,
                uniqueEntityValidator,
                rowVersionValidator,
                isDeletedValidator,
                individualValidator,
                householdHeadValidator);
        ReflectionTestUtils.setField(householdMemberService, "validators", validators);
        lenient().when(householdMemberConfiguration.getCreateTopic()).thenReturn("create-topic");
        lenient().when(householdMemberConfiguration.getUpdateTopic()).thenReturn("update-topic");
        lenient().when(householdMemberConfiguration.getDeleteTopic()).thenReturn("delete-topic");
    }

    private void mockHouseholdFindIds() {
        when(householdService.findById(
                any(List.class),
                any(String.class),
                any(Boolean.class)
        )).thenReturn(
                Collections.singletonList(
                        Household.builder().id("some-household-id").clientReferenceId("some-client-ref-id").build())
        );
    }

    private void mockServiceRequestClientWithIndividual() throws Exception {
        when(serviceRequestClient.fetchResult(
                any(StringBuilder.class),
                any(),
                eq(IndividualResponse.class))
        ).thenReturn(
                IndividualResponse.builder().individual(Collections.singletonList(Individual.builder().build())).build()
        );
    }

    @Test
    @DisplayName("should send data to kafka")
    void shouldSendDataToKafkaTopic() throws Exception {
        HouseholdMemberRequest householdMemberRequest = HouseholdMemberRequestTestBuilder.builder().withHouseholdMember().withRequestInfo()
                .build();

        List<HouseholdMember> createdHouseholdMembers =  householdMemberService.create(householdMemberRequest);

        verify(householdMemberRepository, times(1)).save(createdHouseholdMembers, "create-topic");
    }

    @Test
    @DisplayName("should send data to kafka if the request have the individual who is head of household")
    void shouldSendDataToKafkaTopicWhenIndividualIsHeadOfHousehold() throws Exception {
        HouseholdMemberRequest householdMemberRequest = HouseholdMemberRequestTestBuilder.builder()
                .withHouseholdMember()
                .withHouseholdMemberAsHead()
                .withRequestInfo()
                .build();

        householdMemberService.create(householdMemberRequest);

        verify(householdMemberRepository, times(1)).save(anyList(), anyString());
    }



    @Test
    @DisplayName("should update audit details before pushing the household member to kafka")
    void shouldUpdateAuditDetailsBeforePushingTheHouseholdMemberToKafka() throws Exception {

        HouseholdMemberRequest householdMemberRequest = HouseholdMemberRequestTestBuilder.builder()
                .withHouseholdMemberAsHead()
                .withRequestInfo()
                .build();

        List<HouseholdMember> householdMembers = householdMemberService.create(householdMemberRequest);

        assertNotNull(householdMembers.stream().findAny().get().getAuditDetails().getCreatedBy());
        assertNotNull(householdMembers.stream().findAny().get().getAuditDetails().getCreatedTime());
        assertNotNull(householdMembers.stream().findAny().get().getAuditDetails().getLastModifiedBy());
        assertNotNull(householdMembers.stream().findAny().get().getAuditDetails().getLastModifiedTime());
    }

    @Test
    @DisplayName("should set row version as 1")
    void shouldSetRowVersionAs1AndDeletedAsFalse() throws Exception {

        HouseholdMemberRequest householdMemberRequest = HouseholdMemberRequestTestBuilder.builder()
                .withHouseholdMemberAsHead()
                .withRequestInfo()
                .build();

        List<HouseholdMember> householdMembers = householdMemberService.create(householdMemberRequest);

        assertEquals(1, householdMembers.stream().findAny().get().getRowVersion());
    }

}
