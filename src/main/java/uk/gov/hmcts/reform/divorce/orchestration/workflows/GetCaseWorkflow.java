package uk.gov.hmcts.reform.divorce.orchestration.workflows;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.CaseDataResponse;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.DefaultWorkflow;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.WorkflowException;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.DefaultTaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.Task;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.AddCourtsToPayloadTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.CaseDataToDivorceFormatter;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.GetCase;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.generalorders.GeneralOrdersFilterTask;

import java.util.Map;

import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AUTH_TOKEN_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.CASE_ID_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.CASE_STATE_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.constants.TaskContextConstants.COURT_KEY;

@Component
@RequiredArgsConstructor
public class GetCaseWorkflow extends DefaultWorkflow<Map<String, Object>> {

    private final GetCase getCase;
    private final GeneralOrdersFilterTask generalOrdersFilterTask;
    private final CaseDataToDivorceFormatter caseDataToDivorceFormatter;
    private final AddCourtsToPayloadTask addCourtsToPayloadTask;

    public CaseDataResponse run(String authToken) throws WorkflowException {
        DefaultTaskContext taskContext = new DefaultTaskContext();
        Map<String, Object> caseData = execute(
            new Task[] {
                getCase,
                generalOrdersFilterTask,
                caseDataToDivorceFormatter,
                addCourtsToPayloadTask
            },
            taskContext,
            null,
            ImmutablePair.of(AUTH_TOKEN_JSON_KEY, authToken)
        );

        return CaseDataResponse.builder()
            .caseId(taskContext.getTransientObject(CASE_ID_KEY))
            .state(taskContext.getTransientObject(CASE_STATE_KEY))
            .court(taskContext.getTransientObject(COURT_KEY))
            .data(caseData)
            .build();
    }

}