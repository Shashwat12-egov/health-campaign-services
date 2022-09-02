package org.egov.rn.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.rn.exception.WorkflowException;
import org.egov.rn.repository.ServiceRequestRepository;
import org.egov.rn.service.models.ProcessInstance;
import org.egov.rn.service.models.ProcessInstanceRequest;
import org.egov.rn.service.models.ProcessInstanceResponse;
import org.egov.rn.service.models.State;
import org.egov.rn.web.models.RegistrationRequest;
import org.egov.rn.web.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class WorkflowService {

    private ServiceRequestRepository serviceRequestRepository;
    private ObjectMapper objectMapper;

    @Value("${egov.workflow.host}")
    private String wfHost;

    @Value("${egov.workflow.transition.path}")
    private String wfTransitionUrl;

    @Autowired
    public WorkflowService(ServiceRequestRepository serviceRequestRepository, ObjectMapper objectMapper) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.objectMapper = objectMapper;
    }


    public State updateWorkflowStatus(RegistrationRequest registrationRequest) {
        try {
            ProcessInstance processInstance = getProcessInstanceForRegistration(registrationRequest);
            ProcessInstanceRequest processInstanceRequest = new ProcessInstanceRequest(registrationRequest.getRequestInfo(),
                    Collections.singletonList(processInstance));
            return callWorkflow(processInstanceRequest);
        } catch (Exception ex) {
            throw new WorkflowException(ex.getMessage(), ex);
        }
    }

    private State callWorkflow(ProcessInstanceRequest processInstanceRequest) throws JsonProcessingException {
        StringBuilder url = new StringBuilder(wfHost + wfTransitionUrl);
        log.info(objectMapper.writeValueAsString(processInstanceRequest));
        Object optional = serviceRequestRepository.fetchResult(url, processInstanceRequest);
        ProcessInstanceResponse response = objectMapper.convertValue(optional,
                ProcessInstanceResponse.class);
        return response.getProcessInstances().get(0).getState();
    }

    private ProcessInstance getProcessInstanceForRegistration(RegistrationRequest registrationRequest) {
        List<User> assignes = new ArrayList<>();
        User user = new User();
        user.setUuid(registrationRequest.getRequestInfo().getUserInfo().getUuid());
        assignes.add(user);
        return ProcessInstance.builder()
                .businessId(registrationRequest.getRegistration().getRegistrationId())
                .tenantId(registrationRequest.getRegistration().getTenantId())
                .action("SUBMIT")
                .moduleName("rn-services")
                .businessService("RNS")
                .assignes(assignes)
                .build();
    }
}
