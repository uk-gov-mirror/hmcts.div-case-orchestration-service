package uk.gov.hmcts.reform.divorce.orchestration.framework.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class WorkflowExceptionWithErrors extends WorkflowException {//TODO - in the end, consider if I couldn't reuse the parent class

    private final Map<String, Object> errors;

    public WorkflowExceptionWithErrors(String message, Map<String, Object> errors) {
        super(message);
        this.errors = errors;
    }

    public WorkflowExceptionWithErrors(String message, Map<String, Object> errors, WorkflowException cause) {
        super(message, cause);
        this.errors = errors;
    }

    public Map<String, Object> errors() {//TODO - rename this - do it last
        return Optional.ofNullable(errors).orElse(new HashMap<>());
    }

    public String getCaseId() {
        return null;//TODO - implement this
    }

}