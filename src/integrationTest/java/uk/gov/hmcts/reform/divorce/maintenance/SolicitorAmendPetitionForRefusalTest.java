package uk.gov.hmcts.reform.divorce.maintenance;

import io.restassured.response.Response;
import net.serenitybdd.rest.SerenityRest;
import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackRequest;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackResponse;
import uk.gov.hmcts.reform.divorce.support.CcdSubmissionSupport;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.PETITIONER_SOLICITOR_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.RESPONDENT_SOLICITOR_ORGANISATION_POLICY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AMENDED_CASE_ID_CCD_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CASE_REFERENCE_CCD_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.convertJsonToObject;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.convertObjectToJsonString;

public class SolicitorAmendPetitionForRefusalTest extends CcdSubmissionSupport {

    private static final String ISSUED_SOLICITOR_PETITION_JSON = "solicitor-petition.json";

    @Value("${case.orchestration.solicitor-amend-petition-refusal.context-path}")
    private String solicitorAmendPetitionContextPath;

    @Test
    public void givenValidCase_whenSolicitorAmendPetitionForRefusalRejection_newDraftPetitionIsReturned() throws Exception {
        final UserDetails solicitorUser = createSolicitorUser();
        CaseDetails caseDetails = submitSolicitorCase(ISSUED_SOLICITOR_PETITION_JSON, solicitorUser);

        String originalCaseId = caseDetails.getId().toString();
        CcdCallbackResponse ccdCallbackResponse = amendCase(solicitorUser.getAuthToken(), caseDetails);//TODO - I should check that it returns with no errors

        CaseDetails originalCaseDetails = retrieveCaseForCaseworker(solicitorUser, originalCaseId);//TODO - get caseworker details?
        String amendedCaseId = Optional.ofNullable(originalCaseDetails)
            .map(CaseDetails::getData)
            .map(m -> m.get(AMENDED_CASE_ID_CCD_KEY))
            .map(Map.class::cast)
            .map(m -> m.get(CASE_REFERENCE_CCD_KEY))
            .map(String.class::cast)
            .orElseThrow();
        CaseDetails amendedCaseDetails = retrieveCaseForCaseworker(solicitorUser, amendedCaseId);
        Map<String, Object> amendedCaseDetailsData = amendedCaseDetails.getData();
        assertThat("Amended case should have " + PETITIONER_SOLICITOR_ORGANISATION_POLICY, amendedCaseDetailsData.get(PETITIONER_SOLICITOR_ORGANISATION_POLICY), is(notNullValue()));
        assertThat("Amended case should have " + RESPONDENT_SOLICITOR_ORGANISATION_POLICY, amendedCaseDetailsData.get(RESPONDENT_SOLICITOR_ORGANISATION_POLICY), is(notNullValue()));
    }

    private CcdCallbackResponse amendCase(String authToken, CaseDetails caseDetails) throws Exception {
        String caseId = String.valueOf(caseDetails.getId());
        CcdCallbackRequest ccdCallbackRequest = CcdCallbackRequest.builder()
            .caseDetails(
                uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails.builder()
                    .caseId(caseId)
                    .caseData(caseDetails.getData())
                    .build()
            ).build();
        System.out.println("Case id: " + caseId);//TODO - delete this

        //TODO - use original RestAssured method
//        String json = ResourceLoader.loadJson("fixtures/solicitor/solicitor-request-data-dn-rejection.json");
        Response response = SerenityRest.given()
            .header(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString())//TODO - use string constant
            .header(HttpHeaders.AUTHORIZATION, authToken)
            .body(convertObjectToJsonString(ccdCallbackRequest))
            .when()
            .post(serverUrl + solicitorAmendPetitionContextPath)
            .andReturn();

        assertThat(response.statusCode(), is(HttpStatus.OK.value()));
        CcdCallbackResponse ccdCallbackResponse = convertJsonToObject(response.asString(), CcdCallbackResponse.class);

        assertThat(ccdCallbackResponse.getErrors(), is(nullValue()));
        assertThat(ccdCallbackResponse.getData(), is(notNullValue()));

        return ccdCallbackResponse;
    }

}