package uk.gov.hmcts.reform.divorce.orchestration.util;

import org.mockito.ArgumentMatcher;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.DefaultTaskContext;

import java.util.Map;

import static org.mockito.ArgumentMatchers.argThat;

public class TaskContextMatcher implements ArgumentMatcher<DefaultTaskContext> {

    private final Map<String, Object> expectedMap;

    public TaskContextMatcher(Map<String, Object> expectedMap) {
        this.expectedMap = expectedMap;
    }

    public static DefaultTaskContext aTaskContextWithGivenEntries(Map<String, Object> expectedMap) {
        return argThat(new TaskContextMatcher(expectedMap));
    }

    @Override
    public boolean matches(DefaultTaskContext taskContext) {
        return expectedMap.entrySet().stream()
            .allMatch(expectedEntry -> expectedEntry.getValue().equals(taskContext.getTransientObject(expectedEntry.getKey())));
    }

}