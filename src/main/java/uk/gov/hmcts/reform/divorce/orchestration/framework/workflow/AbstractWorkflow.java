package uk.gov.hmcts.reform.divorce.orchestration.framework.workflow;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.DefaultTaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.Task;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

abstract class AbstractWorkflow<T> implements Workflow<T> {

    private static final String CONTEXT_ERROR_SUFFIX = "_Error";

    private final ThreadLocal<DefaultTaskContext> threadLocalContext = new ThreadLocal<>();

    @Override
    public T execute(Task[] tasks, DefaultTaskContext context, T payload, Pair... pairs) throws WorkflowException {
        setTransientObjectsIntoTaskContext(context, pairs);

        try {
            T payloadToReturn = executeInternal(tasks, payload);

            checkForErrorsSetInContext();

            return payloadToReturn;
        } catch (WorkflowException workflowException) {
            checkForErrorsSetInContext(workflowException);
            throw workflowException;
        } finally {
            threadLocalContext.remove();//TODO - try to comment this and see what fails - something should
        }
    }

    @Override
    public T execute(Task[] tasks, T payload, Pair... pairs) throws WorkflowException {
        return execute(tasks, new DefaultTaskContext(), payload, pairs);
    }

    private void setTransientObjectsIntoTaskContext(DefaultTaskContext context, Pair[] pairs) {
        threadLocalContext.set(context);

        for (Pair pair : pairs) {
            getContext().setTransientObject(pair.getKey().toString(), pair.getValue());
        }
    }

    private void checkForErrorsSetInContext(WorkflowException cause) throws WorkflowExceptionWithErrors {
        Map<String, Object> errors = retrieveErrors();
        if (!errors.isEmpty()) {
            throw new WorkflowExceptionWithErrors("Workflow execution has generated errors", errors, cause);
        }
    }

    private void checkForErrorsSetInContext() throws WorkflowExceptionWithErrors {
        checkForErrorsSetInContext(null);
    }

    private Map<String, Object> retrieveErrors() {
        Map<String, Object> errors = new HashMap<>();

        Set<Map.Entry<String, Object>> entrySet = getContext().getTransientObjects().entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            String key = entry.getKey();
            if (key.endsWith(CONTEXT_ERROR_SUFFIX)) {
                errors.put(key, entry.getValue());
            }
        }

        return errors;
    }

    DefaultTaskContext getContext() {
        return threadLocalContext.get();
    }

    protected abstract T executeInternal(Task[] tasks, T payload) throws WorkflowException;

}