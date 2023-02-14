package org.egov.egovsurveyservices.repository.rowmapper;

import org.egov.egovsurveyservices.web.models.AuditDetails;
import org.egov.egovsurveyservices.web.models.Question;
import org.egov.egovsurveyservices.web.models.SurveyEntity;
import org.egov.egovsurveyservices.web.models.enums.Type;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SurveyRowMapper implements ResultSetExtractor<List<SurveyEntity>>{
    public List<SurveyEntity> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String,SurveyEntity> surveyEntityMap = new LinkedHashMap<>();

        while (rs.next()){
            String uuid = rs.getString("suuid");
            SurveyEntity surveyEntity = surveyEntityMap.get(uuid);

            if(surveyEntity == null) {

                Long lastModifiedTime = rs.getLong("slastmodifiedtime");
                if (rs.wasNull()) {
                    lastModifiedTime = null;
                }

                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("screatedby"))
                        .createdTime(rs.getLong("screatedtime"))
                        .lastModifiedBy(rs.getString("slastmodifiedby"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();

                Array tag = rs.getArray("stag");
                String[] tagArray = (String[])tag.getArray();

                surveyEntity = SurveyEntity.builder()
                        .uuid(rs.getString("suuid"))
                        .tenantId(rs.getString("stenantid"))
                        .title(rs.getString("stitle"))
                        .status(rs.getString("sstatus"))
                        .description(rs.getString("sdescription"))
                        .startDate(rs.getLong("sstartdate"))
                        .endDate(rs.getLong("sendDate"))
                        .postedBy(rs.getString("spostedby"))
                        .collectCitizenInfo(rs.getBoolean("scollectCitizenInfo"))
                        .active(rs.getBoolean("sactive"))
                        .auditDetails(auditdetails)
                        .tag(Arrays.asList(tagArray))
                        .entityType(rs.getString("sentitytype"))
                        .build();
            }
            addChildrenToProperty(rs, surveyEntity);
            Collections.sort(surveyEntity.getQuestions(),
                    Comparator.comparing(Question::getQorder));
            surveyEntityMap.put(uuid, surveyEntity);
        }
        return new ArrayList<>(surveyEntityMap.values());
    }

    private void addChildrenToProperty(ResultSet rs, SurveyEntity surveyEntity)
            throws SQLException {
            addQuestionsToSurvey(rs, surveyEntity);
    }

    private void addQuestionsToSurvey(ResultSet rs, SurveyEntity surveyEntity) throws SQLException {

        String queUuid = rs.getString("quuid");
        String questionId = rs.getString("qsurveyid");
        List<Question> questions = surveyEntity.getQuestions();

        if (!(queUuid != null && questionId.equals(surveyEntity.getUuid())))
            return;

        if (!CollectionUtils.isEmpty(questions))
            for (Question question : questions) {
                if (question.getUuid().equals(queUuid))
                    return;
            }

        AuditDetails auditdetails = AuditDetails.builder()
                .createdBy(rs.getString("qcreatedby"))
                .createdTime(rs.getLong("qcreatedtime"))
                .lastModifiedBy(rs.getString("qlastmodifiedby"))
                .lastModifiedTime(rs.getLong("qlastmodifiedtime"))
                .build();

        Question que =  Question.builder()
                .uuid(rs.getString("quuid"))
                .surveyId(rs.getString("qsurveyid"))
                .questionStatement(rs.getString("qstatement"))
                .status(rs.getString("qstatus"))
                .required(rs.getBoolean("qrequired"))
                .options(Arrays.asList(rs.getString("qoptions").split(",")))
                .type(Type.fromValue(rs.getString("qtype")))
                .qorder(rs.getLong("qorder"))
                .auditDetails(auditdetails)
                .build();

        surveyEntity.addQuestionsItem(que);
    }

}
