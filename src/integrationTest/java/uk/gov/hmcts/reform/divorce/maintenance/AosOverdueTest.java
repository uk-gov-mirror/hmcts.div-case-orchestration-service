package uk.gov.hmcts.reform.divorce.maintenance;

import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.divorce.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.support.cos.RetrieveCaseSupport;
import uk.gov.hmcts.reform.divorce.util.ElasticSearchTestHelper;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.pollinterval.FibonacciPollInterval.fibonacci;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.BAILIFF_SERVICE_SUCCESSFUL;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.SERVED_BY_ALTERNATIVE_METHOD;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.SERVED_BY_PROCESS_SERVER;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_AWAITING;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_DRAFTED;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_OVERDUE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AOS_STARTED;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdStates.AWAITING_DECREE_NISI;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.YES_VALUE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.events.CcdTestEvents.TEST_AOS_AWAITING_EVENT;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.events.CcdTestEvents.TEST_AOS_DRAFTED_EVENT;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.events.CcdTestEvents.TEST_AOS_STARTED_EVENT;

@Slf4j
public class AosOverdueTest extends RetrieveCaseSupport {

    @Value("${case.orchestration.jobScheduler.make-case-overdue-for-aos.context-path}")
    private String jobSchedulerContextPath;

    private static final String SUBMIT_COMPLETE_CASE_JSON_FILE_PATH = "submit-complete-case.json";

    private String aosAwaitingCaseId;
    private String aosStartedCaseId;
    private String servedByProcessServerCaseId;
    private String servedByAlternativeMethodCaseId;
    private String servedByBailiffCaseId;
    private String aosDraftedCaseId;
    private String aosDraftedServedByProcessServerCaseId;
    private String aosDraftedServedByAlternativeMethodCaseId;

    private UserDetails citizenUser;
    private UserDetails caseworkerUser;

    @Autowired
    private ElasticSearchTestHelper elasticSearchTestHelper;

    @Before
    public void setUp() {
        citizenUser = createCitizenUser();
        caseworkerUser = createCaseWorkerUser();

        aosAwaitingCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_AWAITING_EVENT);
        aosStartedCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_STARTED_EVENT);
        servedByProcessServerCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_STARTED_EVENT, Pair.of(SERVED_BY_PROCESS_SERVER, YES_VALUE));
        servedByAlternativeMethodCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_STARTED_EVENT, Pair.of(SERVED_BY_ALTERNATIVE_METHOD, YES_VALUE));
        servedByBailiffCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_AWAITING_EVENT, Pair.of(BAILIFF_SERVICE_SUCCESSFUL, YES_VALUE));

        aosDraftedCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_DRAFTED_EVENT);
        aosDraftedServedByProcessServerCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_DRAFTED_EVENT,
            Pair.of(SERVED_BY_PROCESS_SERVER, YES_VALUE));
        aosDraftedServedByAlternativeMethodCaseId = createCaseAndTriggerGivenEvent(TEST_AOS_DRAFTED_EVENT,
            Pair.of(SERVED_BY_ALTERNATIVE_METHOD, YES_VALUE));

        elasticSearchTestHelper.ensureCaseIsSearchable(aosAwaitingCaseId, caseworkerUser.getAuthToken(), AOS_AWAITING);
        elasticSearchTestHelper.ensureCaseIsSearchable(aosStartedCaseId, caseworkerUser.getAuthToken(), AOS_STARTED);
        elasticSearchTestHelper.ensureCaseIsSearchable(servedByProcessServerCaseId, caseworkerUser.getAuthToken(), AOS_STARTED);
        elasticSearchTestHelper.ensureCaseIsSearchable(servedByAlternativeMethodCaseId, caseworkerUser.getAuthToken(), AOS_STARTED);
        elasticSearchTestHelper.ensureCaseIsSearchable(servedByBailiffCaseId, caseworkerUser.getAuthToken(), AOS_AWAITING);

        elasticSearchTestHelper.ensureCaseIsSearchable(aosDraftedCaseId, caseworkerUser.getAuthToken(), AOS_DRAFTED);
        elasticSearchTestHelper.ensureCaseIsSearchable(aosDraftedServedByProcessServerCaseId, caseworkerUser.getAuthToken(), AOS_DRAFTED);
        elasticSearchTestHelper.ensureCaseIsSearchable(aosDraftedServedByAlternativeMethodCaseId, caseworkerUser.getAuthToken(), AOS_DRAFTED);
    }

    private String createCaseAndTriggerGivenEvent(String eventId, Pair<String, Object>... additionalCaseData) {
        final CaseDetails caseDetails = submitCase(SUBMIT_COMPLETE_CASE_JSON_FILE_PATH, citizenUser);
        String caseId = String.valueOf(caseDetails.getId());
        log.debug("Created case id {}", caseId);
        updateCase(caseId, null, eventId, caseworkerUser, additionalCaseData);

        return caseId;
    }

    @Test
    public void shouldMoveEligibleCasesWhenAosIsOverdue() {
        RestAssured
            .given()
            .header(HttpHeaders.AUTHORIZATION, caseworkerUser.getAuthToken())
            .when()
            .post(serverUrl + jobSchedulerContextPath)
            .then()
            .statusCode(HttpStatus.SC_OK);

        await().pollInterval(fibonacci(SECONDS)).atMost(480, SECONDS).untilAsserted(() -> {
            assertCaseIsInExpectedState(aosAwaitingCaseId, AOS_OVERDUE);
            assertCaseIsInExpectedState(aosStartedCaseId, AOS_STARTED);
            assertCaseIsInExpectedState(servedByProcessServerCaseId, AWAITING_DECREE_NISI);
            assertCaseIsInExpectedState(servedByAlternativeMethodCaseId, AWAITING_DECREE_NISI);
            assertCaseIsInExpectedState(servedByBailiffCaseId, AWAITING_DECREE_NISI);
            assertCaseIsInExpectedState(aosDraftedCaseId, AOS_OVERDUE);
            assertCaseIsInExpectedState(aosDraftedServedByProcessServerCaseId, AWAITING_DECREE_NISI);
            assertCaseIsInExpectedState(aosDraftedServedByAlternativeMethodCaseId, AWAITING_DECREE_NISI);
        });
    }

    private void assertCaseIsInExpectedState(String caseId, String expectedState) {
        CaseDetails caseDetails = retrieveCaseForCitizen(citizenUser, caseId);
        String state = caseDetails.getState();
        assertThat(format("Case %s should be in \"%s\" state", caseId, expectedState), state, is(expectedState));
    }

}