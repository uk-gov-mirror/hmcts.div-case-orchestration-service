package uk.gov.hmcts.reform.divorce.orchestration.tasks;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.divorce.orchestration.client.CaseMaintenanceClient;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.exception.CaseNotFoundException;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.DefaultTaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskException;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_COURT;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_STATE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AUTH_TOKEN_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_DIVORCE_UNIT;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.CASE_ID_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.CASE_STATE_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.CCD_CASE_DATA;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.COURT_KEY;

@RunWith(MockitoJUnitRunner.class)
public class GetCaseUTest {

    @Mock
    private CaseMaintenanceClient caseMaintenanceClient;

    @InjectMocks
    private GetCase classUnderTest;

    private DefaultTaskContext context;

    @Before
    public void setUp() {
        context = new DefaultTaskContext();
        context.setTransientObject(AUTH_TOKEN_JSON_KEY, AUTH_TOKEN);
    }

    @Test
    public void givenNoCaseExists_whenGetCase_thenReturnThrowException() {
        when(caseMaintenanceClient.getCase(AUTH_TOKEN)).thenReturn(null);

        TaskException taskException = assertThrows(TaskException.class, () -> classUnderTest.execute(context, null));

        assertThat(taskException.getCause(), is(instanceOf(CaseNotFoundException.class)));
        verify(caseMaintenanceClient).getCase(AUTH_TOKEN);
    }

    @Test
    public void givenCaseExists_whenGetCase_thenReturnExpectedOutput() throws TaskException {
        final Map<String, Object> caseData = singletonMap(D_8_DIVORCE_UNIT, TEST_COURT);
        final CaseDetails cmsResponse =
            CaseDetails.builder()
                .caseData(caseData)
                .caseId(TEST_CASE_ID)
                .state(TEST_STATE)
                .build();
        when(caseMaintenanceClient.getCase(AUTH_TOKEN)).thenReturn(cmsResponse);

        Map<String, Object> returnedCaseData = classUnderTest.execute(context, null);

        assertThat(returnedCaseData, is(caseData));
        assertThat(context.getTransientObject(CCD_CASE_DATA), is(caseData));
        assertThat(context.getTransientObject(CASE_ID_KEY), is(TEST_CASE_ID));
        assertThat(context.getTransientObject(CASE_STATE_KEY), is(TEST_STATE));
        assertThat(context.getTransientObject(COURT_KEY), is(TEST_COURT));
        verify(caseMaintenanceClient).getCase(AUTH_TOKEN);
    }

}