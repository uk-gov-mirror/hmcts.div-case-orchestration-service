package uk.gov.hmcts.reform.divorce.maintenance;

import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackResponse;
import uk.gov.hmcts.reform.divorce.support.CcdSubmissionSupport;
import uk.gov.hmcts.reform.divorce.util.ResourceLoader;
import uk.gov.hmcts.reform.divorce.util.RestUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.PETITIONER_SOLICITOR_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.RESPONDENT_SOLICITOR_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AMENDED_CASE_ID_CCD_KEY;

public class SolicitorAmendPetitionForRefusalTest extends CcdSubmissionSupport {
    private static final String PAYLOAD_CONTEXT_PATH = "fixtures/solicitor/";

    @Value("${case.orchestration.solicitor-amend-petition-refusal.context-path}")
    private String solicitorAmendPetitionContextPath;

    @Test
    public void givenValidCase_whenSolicitorAmendPetitionForRefusalRejection_newDraftPetitionIsReturned() throws Exception {
        final UserDetails solicitorUser = createSolicitorUser();

        CcdCallbackResponse ccdCallbackResponse = postWithData("solicitor-request-data-dn-rejection.json", solicitorUser.getAuthToken());
        String amendedCaseId = Optional.ofNullable(ccdCallbackResponse)
            .map(CcdCallbackResponse::getData)
            .map(m -> m.get(AMENDED_CASE_ID_CCD_KEY))
            .map(String.class::cast)
            .orElseThrow();

        CaseDetails amendedCaseDetails = retrieveCase(solicitorUser, amendedCaseId);
        assertThat(amendedCaseDetails.getData().get(PETITIONER_SOLICITOR_ORGANISATION_POLICY), is(notNullValue()));
        assertThat(amendedCaseDetails.getData().get(RESPONDENT_SOLICITOR_ORGANISATION_POLICY), is(notNullValue()));
    }

    private CcdCallbackResponse postWithData(String pathToFileWithData, String authToken) throws Exception {
        final Map<String, Object> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
        headers.put(HttpHeaders.AUTHORIZATION, authToken);

        return RestUtil.postToRestService(
            serverUrl + solicitorAmendPetitionContextPath,
            headers,
            ResourceLoader.loadJson(PAYLOAD_CONTEXT_PATH + pathToFileWithData)
        ).then()
            .statusCode(HttpStatus.OK.value())
            .and()
            .extract().body().as(CcdCallbackResponse.class);
    }

}