package uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task;

public class TaskException extends RuntimeException {

    public TaskException(String message) {
        super(message);
    }

    public TaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public TaskException(Throwable cause) {
        super(cause);//TODO - maybe we don't want this constructor. It's leaking code information. - We only want the message (and the cause, of course) - do it last
    }

}