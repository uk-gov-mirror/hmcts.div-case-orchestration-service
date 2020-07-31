package uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task;

import java.util.Optional;

public class TaskException extends RuntimeException {

    private Optional<String> caseId = Optional.empty();//TODO - should this be in a constructor? or maybe in the getter?

    public TaskException(String message) {
        super(message);
    }

    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskException(Throwable cause) {
        super(cause);//TODO - maybe we don't want this constructor. It's leaking code information. - We only want the message (and the cause, of course)
    }

    public Optional<String> getCaseId() {
        return caseId;
    }

}