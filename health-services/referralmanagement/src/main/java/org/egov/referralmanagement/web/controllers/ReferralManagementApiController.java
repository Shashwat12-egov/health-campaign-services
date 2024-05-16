package org.egov.referralmanagement.web.controllers;

import io.swagger.annotations.ApiParam;
import org.egov.common.models.core.URLParams;
import org.egov.common.models.core.SearchResponse;
import org.egov.referralmanagement.service.ReferralManagementService;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.common.models.referralmanagement.Referral;
import org.egov.common.models.referralmanagement.ReferralBulkRequest;
import org.egov.common.models.referralmanagement.ReferralBulkResponse;
import org.egov.common.models.referralmanagement.ReferralRequest;
import org.egov.common.models.referralmanagement.ReferralResponse;
import org.egov.common.models.referralmanagement.ReferralSearchRequest;
import org.egov.common.producer.Producer;
import org.egov.common.utils.ResponseInfoFactory;
import org.egov.referralmanagement.config.ReferralManagementConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * Referral Management Api Controller
 */
@Controller
@RequestMapping("")
@Validated
public class ReferralManagementApiController {
    private final HttpServletRequest httpServletRequest;

    private final ReferralManagementService referralManagementService;

    private final Producer producer;

    private final ReferralManagementConfiguration referralManagementConfiguration;

    public ReferralManagementApiController(
            HttpServletRequest httpServletRequest, 
            ReferralManagementService referralManagementService, 
            Producer producer,
            ReferralManagementConfiguration referralManagementConfiguration
    ) {
        this.httpServletRequest = httpServletRequest;
        this.referralManagementService = referralManagementService;
        this.producer = producer;
        this.referralManagementConfiguration = referralManagementConfiguration;
    }

    /**
     * @
     * @param request
     * @return
     */
    @RequestMapping(value = "/v1/_create", method = RequestMethod.POST)
    public ResponseEntity<ReferralResponse> referralV1CreatePost(@ApiParam(value = "Capture details of Referral", required = true) @Valid @RequestBody ReferralRequest request) {

        Referral referral = referralManagementService.create(request);
        ReferralResponse response = ReferralResponse.builder()
                .referral(referral)
                .responseInfo(ResponseInfoFactory
                        .createResponseInfo(request.getRequestInfo(), true))
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }


    /**
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/v1/bulk/_create", method = RequestMethod.POST)
    public ResponseEntity<ResponseInfo> referralBulkV1CreatePost(@ApiParam(value = "Capture details of Referral", required = true) @Valid @RequestBody ReferralBulkRequest request) {
        request.getRequestInfo().setApiId(httpServletRequest.getRequestURI());
        referralManagementService.putInCache(request.getReferrals());
        producer.push(referralManagementConfiguration.getCreateReferralBulkTopic(), request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseInfoFactory
                .createResponseInfo(request.getRequestInfo(), true));
    }

    /**
     *
     * @param request
     * @param searchCriteria Additional common search criteria.
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/v1/_search", method = RequestMethod.POST)
    public ResponseEntity<ReferralBulkResponse> referralV1SearchPost(@ApiParam(value = "Referral Search.", required = true) @Valid @RequestBody ReferralSearchRequest request,
                                                                     ) throws Exception {

        List<Referral> referrals = referralManagementService.search(request, searchCriteria.getLimit(), searchCriteria.getOffset(), searchCriteria.getTenantId(), searchCriteria.getLastChangedSince(), searchCriteria.getIncludeDeleted());
        SearchResponse<Referral> referralSearchResponse = referralManagementService.search(request, limit, offset, tenantId, lastChangedSince, includeDeleted);
        ReferralBulkResponse response = ReferralBulkResponse.builder().responseInfo(ResponseInfoFactory
                .createResponseInfo(request.getRequestInfo(), true))
                .referrals(referralSearchResponse.getResponse())
                .totalCount(referralSearchResponse.getTotalCount())
                .build();

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/v1/_update", method = RequestMethod.POST)
    public ResponseEntity<ReferralResponse> referralV1UpdatePost(@ApiParam(value = "Capture details of Existing Referral", required = true) @Valid @RequestBody ReferralRequest request) {
        Referral referral = referralManagementService.update(request);

        ReferralResponse response = ReferralResponse.builder()
                .referral(referral)
                .responseInfo(ResponseInfoFactory
                        .createResponseInfo(request.getRequestInfo(), true))
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

    }

    /**
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "/v1/bulk/_update", method = RequestMethod.POST)
    public ResponseEntity<ResponseInfo> referralV1BulkUpdatePost(@ApiParam(value = "Capture details of Existing Referral", required = true) @Valid @RequestBody ReferralBulkRequest request) {
        request.getRequestInfo().setApiId(httpServletRequest.getRequestURI());
        producer.push(referralManagementConfiguration.getUpdateReferralBulkTopic(), request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseInfoFactory
                .createResponseInfo(request.getRequestInfo(), true));
    }

    @RequestMapping(value = "/v1/_delete", method = RequestMethod.POST)
    public ResponseEntity<ReferralResponse> referralV1DeletePost(@ApiParam(value = "Capture details of Existing Referral", required = true) @Valid @RequestBody ReferralRequest request) {
        Referral referral = referralManagementService.delete(request);

        ReferralResponse response = ReferralResponse.builder()
                .referral(referral)
                .responseInfo(ResponseInfoFactory
                        .createResponseInfo(request.getRequestInfo(), true))
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

    }

    @RequestMapping(value = "/v1/bulk/_delete", method = RequestMethod.POST)
    public ResponseEntity<ResponseInfo> referralV1BulkDeletePost(@ApiParam(value = "Capture details of Existing Referral", required = true) @Valid @RequestBody ReferralBulkRequest request) {
        request.getRequestInfo().setApiId(httpServletRequest.getRequestURI());
        producer.push(referralManagementConfiguration.getDeleteReferralBulkTopic(), request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ResponseInfoFactory
                .createResponseInfo(request.getRequestInfo(), true));
    }

}
