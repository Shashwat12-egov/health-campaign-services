package org.egov.egovsurveyservices.service;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.egovsurveyservices.producer.Producer;
import org.egov.egovsurveyservices.repository.SurveyRepository;
import org.egov.egovsurveyservices.utils.SurveyUtil;
import org.egov.egovsurveyservices.validators.SurveyValidator;
import org.egov.egovsurveyservices.web.models.Answer;
import org.egov.egovsurveyservices.web.models.AnswerEntity;
import org.egov.egovsurveyservices.web.models.AnswerRequest;
import org.egov.egovsurveyservices.web.models.AnswerResponse;
import org.egov.egovsurveyservices.web.models.Question;
import org.egov.egovsurveyservices.web.models.SurveyEntity;
import org.egov.egovsurveyservices.web.models.SurveyRequest;
import org.egov.egovsurveyservices.web.models.SurveyResultsSearchCriteria;
import org.egov.egovsurveyservices.web.models.SurveySearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.egov.egovsurveyservices.utils.SurveyServiceConstants.ACTIVE;
import static org.egov.egovsurveyservices.utils.SurveyServiceConstants.CITIZEN;
import static org.egov.egovsurveyservices.utils.SurveyServiceConstants.INACTIVE;

@Slf4j
@Service
public class SurveyService {

    @Autowired
    private SurveyValidator surveyValidator;

    @Autowired
    private Producer producer;

    @Autowired
    private EnrichmentService enrichmentService;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyUtil surveyUtil;

    public SurveyEntity createSurvey(SurveyRequest surveyRequest) {

        SurveyEntity surveyEntity = surveyRequest.getSurveyEntity();

        // Validate whether usertype employee is trying to create survey.
        surveyValidator.validateUserType(surveyRequest.getRequestInfo());
        // Validate question types.
        surveyValidator.validateQuestions(surveyEntity);
        // Validate survey uniqueness.
        surveyValidator.validateSurveyUniqueness(surveyEntity);

        // Persist survey if it passes all validations
        List<String> listOfTenantIds = new ArrayList<>(surveyEntity.getTenantIds());
        Integer countOfSurveyEntities = listOfTenantIds.size();
        List<String> listOfSurveyIds = surveyUtil.getIdList(surveyRequest.getRequestInfo(), listOfTenantIds.get(0), "ss.surveyid", "SY-[cy:yyyy-MM-dd]-[SEQ_EG_DOC_ID]", countOfSurveyEntities);
        log.info(listOfSurveyIds.toString());
        for(int i = 0; i < countOfSurveyEntities; i++){
            surveyEntity.setUuid(listOfSurveyIds.get(i));
            surveyEntity.setTenantId(listOfTenantIds.get(i));
            // Enrich survey entity
            enrichmentService.enrichSurveyEntity(surveyRequest);
            log.info(surveyRequest.getSurveyEntity().toString());
            producer.push("save-ss-survey", Collections.singletonList(surveyRequest.getSurveyEntity()));
        }

        return surveyEntity;
    }

    public List<SurveyEntity> searchSurveys(SurveySearchCriteria criteria, Boolean isCitizen) {
        List<String> listOfSurveyIds = surveyRepository.fetchSurveyUuids(criteria);

        if(CollectionUtils.isEmpty(listOfSurveyIds))
            return new ArrayList<>();

        criteria.setListOfSurveyIds(listOfSurveyIds);
        List<SurveyEntity> surveyEntities = surveyRepository.fetchSurveys(criteria);

        if(CollectionUtils.isEmpty(surveyEntities))
            return new ArrayList<>();

        enrichNumberOfResponsesForEachSurvey(listOfSurveyIds, surveyEntities);

        if(isCitizen)
            enrichWhetherCitizenHasResponded(surveyEntities, listOfSurveyIds, criteria.getCitizenId());

        postProcessSurveySearchResults(surveyEntities);

        return surveyEntities;
    }

    private void enrichWhetherCitizenHasResponded(List<SurveyEntity> surveyEntities, List<String> listOfSurveyIds, String citizenId) {
        List<String> surveyIdsForWhichCitizenHasResponded = surveyRepository.fetchSurveyIdsForWhichCitizenResponded(listOfSurveyIds, citizenId);
        Set<String> surveyIdsHashSet = new HashSet<>(surveyIdsForWhichCitizenHasResponded);
        surveyEntities.forEach(surveyEntity -> {
            if(surveyIdsHashSet.contains(surveyEntity.getUuid()))
                surveyEntity.setHasResponded(Boolean.TRUE);
            else
                surveyEntity.setHasResponded(Boolean.FALSE);
        });
    }

    private void postProcessSurveySearchResults(List<SurveyEntity> surveyEntities) {
        Long currentTime = System.currentTimeMillis();
        surveyEntities.forEach(surveyEntity -> {
            if(surveyEntity.getStatus().equals(ACTIVE)){
                if(currentTime < surveyEntity.getStartDate() || currentTime > surveyEntity.getEndDate())
                    surveyEntity.setStatus(INACTIVE);
            }
        });
    }

    private void enrichNumberOfResponsesForEachSurvey(List<String> listOfSurveyIds, List<SurveyEntity> surveyEntities) {
        List<Map<String, Object>> surveyIdToResponseCountList = (List<Map<String, Object>>) surveyRepository.fetchCountMapForGivenSurveyIds(listOfSurveyIds);
        //log.info(surveyIdToResponseCountList.toString());
        Map<Object, Object> surveyIdToResponseCountMap = new HashMap<>();
        for(Map m : surveyIdToResponseCountList){
            surveyIdToResponseCountMap.put(m.get("surveyid"), m.get("count"));
        }
        surveyEntities.forEach(entity -> {
            if(surveyIdToResponseCountMap.containsKey(entity.getUuid()))
                entity.setAnswersCount((Long)surveyIdToResponseCountMap.get(entity.getUuid()));
            else
                entity.setAnswersCount(0l);
        });
    }

    public void submitResponse(AnswerRequest answerRequest) {
        RequestInfo requestInfo = answerRequest.getRequestInfo();
        AnswerEntity answerEntity = answerRequest.getAnswerEntity();

        // Validations

        // 1. Validate whether userType is citizen or not
        surveyValidator.validateUserTypeForAnsweringSurvey(requestInfo);
        // 2. Validate if survey for which citizen is responding exists
        if(CollectionUtils.isEmpty(surveyRepository.fetchSurveys(SurveySearchCriteria.builder().isCountCall(Boolean.FALSE).uuid(answerEntity.getSurveyId()).build())))
            throw new CustomException("EG_SY_DOES_NOT_EXIST_ERR", "The survey for which citizen responded does not exist");
        // 3. Validate if citizen has already responded or not
        surveyValidator.validateWhetherCitizenAlreadyResponded(answerEntity, requestInfo.getUserInfo().getUuid());
        // 4. Validate answers
        surveyValidator.validateAnswers(answerEntity);
        
        Boolean collectCitizenInfo = fetchSurveyAnonymitySetting(answerEntity.getSurveyId());

        // Enrich answer request
        enrichmentService.enrichAnswerEntity(answerRequest, collectCitizenInfo);

        // Persist response if it passes all validations
        producer.push("save-ss-answer", answerRequest);
    }

    private Boolean fetchSurveyAnonymitySetting(String surveyId) {
        if(ObjectUtils.isEmpty(surveyId))
            throw new CustomException("EG_SY_ANONYMITY_SETTING_FETCH_ERR", "Cannot fetch anonymity setting if surveyId is empty or null");
        return surveyRepository.fetchAnonymitySetting(surveyId);
    }

    public List<Question> fetchQuestionListBasedOnSurveyId(String surveyId) {
        List<Question> questionList = surveyRepository.fetchQuestionsList(surveyId);
        if(CollectionUtils.isEmpty(questionList))
            return new ArrayList<>();
        return questionList;
    }

    public boolean hasCitizenAlreadyResponded(AnswerEntity answerEntity, String citizenId) {
        if(ObjectUtils.isEmpty(answerEntity.getSurveyId()))
            throw new CustomException("EG_SY_FETCH_CITIZEN_RESP_ERR", "Cannot fetch citizen's response without surveyId");
        return surveyRepository.fetchWhetherCitizenAlreadyResponded(answerEntity.getSurveyId(), citizenId);
    }

    public SurveyEntity updateSurvey(SurveyRequest surveyRequest) {
        SurveyEntity surveyEntity = surveyRequest.getSurveyEntity();
        RequestInfo requestInfo = surveyRequest.getRequestInfo();
        // Validate survey existence
        SurveyEntity existingSurveyEntity = surveyValidator.validateSurveyExistence(surveyEntity);
        // Validate whether usertype employee is trying to update survey.
        surveyValidator.validateUserType(surveyRequest.getRequestInfo());
        // Validate question types.
        surveyValidator.validateQuestions(surveyEntity);
        surveyValidator.validateUpdateRequest(surveyEntity);
        // Enrich update request
        surveyEntity.setAuditDetails(existingSurveyEntity.getAuditDetails());
        surveyEntity.getQuestions().forEach(question -> {
            question.setAuditDetails(existingSurveyEntity.getQuestions().get(0).getAuditDetails());
        });

        surveyEntity.setPostedBy(requestInfo.getUserInfo().getName());
        surveyEntity.getAuditDetails().setLastModifiedBy(requestInfo.getUserInfo().getUuid());
        surveyEntity.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
        surveyEntity.getQuestions().forEach(question -> {
            question.getAuditDetails().setLastModifiedTime(System.currentTimeMillis());
            question.getAuditDetails().setLastModifiedBy(requestInfo.getUserInfo().getUuid());
        });

        // Update survey if it passes all validations
        producer.push("update-ss-survey", Collections.singletonList(surveyRequest.getSurveyEntity()));

        return surveyEntity;
    }

    public void deleteSurvey(SurveyRequest surveyRequest) {
        SurveyEntity surveyEntity = surveyRequest.getSurveyEntity();

        // Validate survey existence
        surveyValidator.validateSurveyExistence(surveyEntity);
        // Validate whether usertype employee is trying to delete survey.
        surveyValidator.validateUserType(surveyRequest.getRequestInfo());

        surveyEntity.setActive(Boolean.FALSE);
        surveyEntity.setStatus(INACTIVE);

        producer.push("delete-ss-survey", surveyRequest);

    }

    public AnswerResponse fetchSurveyResults(RequestInfo requestInfo, SurveyResultsSearchCriteria criteria) {

        // Validate whether employee is trying to fetch survey results
        //surveyValidator.validateUserType(requestInfo);

        // Validate survey exists
        List<SurveyEntity> surveyEntities = surveyRepository.fetchSurveys(SurveySearchCriteria.builder().isCountCall(Boolean.FALSE).uuid(criteria.getSurveyId()).build());

        if(CollectionUtils.isEmpty(surveyEntities))
            throw new CustomException("EG_SY_DOES_NOT_EXIST_ERR", "The provided survey does not exist");

        List<Answer> answers = new ArrayList<>();
        List<String> listOfCitizensWhoResponded = new ArrayList<>();

        // If citizen is accessing API, enrich his uuid in answers being fetched, in case of employee, fetch all citizens who responded
        if(requestInfo.getUserInfo().getType().equals(CITIZEN))
            listOfCitizensWhoResponded = Collections.singletonList(requestInfo.getUserInfo().getUuid());
        else
            listOfCitizensWhoResponded = surveyRepository.fetchCitizensUuid(criteria);

        log.info(listOfCitizensWhoResponded.toString());

        // Fetch answers given by the fetched citizens for the requested survey
        answers = surveyRepository.fetchSurveyResults(SurveyResultsSearchCriteria.builder().citizenUuids(listOfCitizensWhoResponded).surveyId(criteria.getSurveyId()).build());

        AnswerResponse response = AnswerResponse.builder()
                                                .answers(answers)
                                                .surveyId(surveyEntities.get(0).getUuid())
                                                .title(surveyEntities.get(0).getTitle())
                                                .tenantId(surveyEntities.get(0).getTenantId())
                                                .description(surveyEntities.get(0).getDescription())
                                                .build();
        return response;
    }

    public Integer countTotalSurveys(SurveySearchCriteria criteria) {
        return surveyRepository.fetchTotalSurveyCount(criteria);
    }
}
