package uk.gov.hmcts.reform.divorce.orchestration.workflows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.WorkflowException;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.BulkCaseCreate;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SearchAwaitingPronouncementCases;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.UpdateDivorceCaseWithinBulk;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_PAYLOAD_TO_RETURN;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AUTH_TOKEN_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.util.TaskContextMatcher.aTaskContextWithGivenEntries;

@RunWith(MockitoJUnitRunner.class)
public class ProcessAwaitingPronouncementCasesWorkflowTest {

    @Mock
    private SearchAwaitingPronouncementCases searchAwaitingPronouncementCasesMock;

    @Mock
    private BulkCaseCreate createBulkCaseMock;

    @Mock
    private UpdateDivorceCaseWithinBulk updateDivorceCaseWithinBulkMock;

    @InjectMocks
    private ProcessAwaitingPronouncementCasesWorkflow classUnderTest;

    @Test
    public void whenProcessAwaitingPronouncement_thenProcessAsExpected() throws WorkflowException {
        when(searchAwaitingPronouncementCasesMock.execute(any(), isNull())).thenReturn(null);
        when(createBulkCaseMock.execute(any(), isNull())).thenReturn(TEST_PAYLOAD_TO_RETURN);
        when(updateDivorceCaseWithinBulkMock.execute(any(), eq(TEST_PAYLOAD_TO_RETURN))).thenReturn(TEST_PAYLOAD_TO_RETURN);

        Map<String, Object> returnedCaseData = classUnderTest.run(AUTH_TOKEN);

        assertThat(returnedCaseData, is(TEST_PAYLOAD_TO_RETURN));
        Map<String, Object> expectedTaskProperties = Map.of(AUTH_TOKEN_JSON_KEY, AUTH_TOKEN);
        verify(searchAwaitingPronouncementCasesMock).execute(aTaskContextWithGivenEntries(expectedTaskProperties), isNull());
        verify(createBulkCaseMock).execute(aTaskContextWithGivenEntries(expectedTaskProperties), isNull());
        verify(updateDivorceCaseWithinBulkMock).execute(aTaskContextWithGivenEntries(expectedTaskProperties), eq(TEST_PAYLOAD_TO_RETURN));
    }

}
