package uk.gov.hmcts.reform.divorce.maintenance;

import io.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.category.ExtendedTest;
import uk.gov.hmcts.reform.divorce.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.support.CcdSubmissionSupport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_AWAITING_SOLICITOR;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_COMPLETED;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_SUBMITTED_AWAITING_ANSWER;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AWAITING_DECREE_NISI;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CASE_STATE_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CCD_CASE_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.RECEIVED_AOS_FROM_RESP;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.RECEIVED_AOS_FROM_RESP_DATE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.YES_VALUE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.events.CcdTestEvents.TEST_AOS_STARTED_EVENT;
import static uk.gov.hmcts.reform.divorce.util.DateConstants.CCD_DATE_FORMAT;
import static uk.gov.hmcts.reform.divorce.util.ResourceLoader.loadJson;

public class SubmitRespondentAosCaseTest extends CcdSubmissionSupport {
    private static final String PAYLOAD_CONTEXT_PATH = "fixtures/maintenance/submit-aos/";
    private static final String AOS_DEFEND_CONSENT_JSON = "aos-defend-consent.json";
    private static final String AOS_DEFEND_NO_CONSENT_JSON = "aos-defend-no-consent.json";
    private static final String AOS_NO_DEFEND_CONSENT_JSON = "aos-no-defend-consent.json";
    private static final String AOS_SOLICITOR_REPRESENTATION_JSON = "aos-solicitor-representation.json";
    private static final String SUBMIT_COMPLETE_CASE_JSON = "submit-unlinked-case.json";
    private static final String SUBMIT_COMPLETE_CASE_REASON_ADULTERY_JSON = "submit-complete-case-reason-adultery.json";
    private static final String SUBMIT_COMPLETE_CASE_REASON_2_YEAR_SEP_JSON = "submit-complete-case-reason-2yearSep.json";
    private static final String AOS_NO_DEFEND_NO_CONSENT_JSON;
    private UserDetails userDetails;

    static {
        try {
            AOS_NO_DEFEND_NO_CONSENT_JSON = loadJson(PAYLOAD_CONTEXT_PATH + "aos-no-defend-no-consent.json");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup() {
        userDetails = createCitizenUser();
    }

    @Test
    public void givenConsentAndDefend_whenSubmitAos_thenProceedAsExpected() throws Exception {
        CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            loadJson(PAYLOAD_CONTEXT_PATH + AOS_DEFEND_CONSENT_JSON));

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AOS_SUBMITTED_AWAITING_ANSWER));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    @Category(ExtendedTest.class)
    public void givenNoConsentAndDefend_whenSubmitAos_thenProceedAsExpected() throws Exception {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            loadJson(PAYLOAD_CONTEXT_PATH + AOS_DEFEND_NO_CONSENT_JSON));

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AOS_SUBMITTED_AWAITING_ANSWER));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    @Category(ExtendedTest.class)
    public void givenConsentAndNoDefend_whenSubmitAos_thenProceedAsExpected() throws Exception {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            loadJson(PAYLOAD_CONTEXT_PATH + AOS_NO_DEFEND_CONSENT_JSON));

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AWAITING_DECREE_NISI));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    @Category(ExtendedTest.class)
    public void givenNoConsentAndNoDefendAndReasonIsNotAdultery_thenProceedAsExpected() {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            AOS_NO_DEFEND_NO_CONSENT_JSON);

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AWAITING_DECREE_NISI));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    @Category(ExtendedTest.class)
    public void givenNoConsentAndNoDefendAndReasonIsAdultery_thenProceedAsExpected() {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_REASON_ADULTERY_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            AOS_NO_DEFEND_NO_CONSENT_JSON);

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AOS_COMPLETED));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    @Category(ExtendedTest.class)
    public void givenNoConsentAndNoDefendAndReasonIs2YearSeparation_thenProceedAsExpected() {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_REASON_2_YEAR_SEP_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            AOS_NO_DEFEND_NO_CONSENT_JSON);

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AOS_COMPLETED));
        assertAosSubmittedData(userDetails, caseDetails.getId().toString());
    }

    @Test
    public void givenRespondentSolicitorRepresented_whenSubmitAos_thenProceedAsExpected() throws Exception {
        CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON, userDetails);

        updateCaseForCitizen(String.valueOf(caseDetails.getId()),
            null,
            TEST_AOS_STARTED_EVENT,
            userDetails);

        Response cosResponse = submitRespondentAosCase(userDetails.getAuthToken(), caseDetails.getId(),
            loadJson(PAYLOAD_CONTEXT_PATH + AOS_SOLICITOR_REPRESENTATION_JSON));

        assertThat(cosResponse.getStatusCode(), is(OK.value()));
        assertThat(cosResponse.path(CCD_CASE_ID), is(caseDetails.getId()));
        assertThat(cosResponse.path(CASE_STATE_JSON_KEY), is(AOS_AWAITING_SOLICITOR));
        assertThat(caseDetails.getData().get(RECEIVED_AOS_FROM_RESP), is(nullValue()));
        assertThat(caseDetails.getData().get(RECEIVED_AOS_FROM_RESP_DATE), is(nullValue()));
    }

    private void assertAosSubmittedData(UserDetails userDetails, String caseId) {
        CaseDetails caseDetails = this.retrieveCaseForCitizen(userDetails, caseId);
        assertThat(caseDetails.getData().get(RECEIVED_AOS_FROM_RESP), is(YES_VALUE));
        assertThat(caseDetails.getData().get(RECEIVED_AOS_FROM_RESP_DATE), is(LocalDate.now().format(DateTimeFormatter.ofPattern(CCD_DATE_FORMAT))));
    }
}
