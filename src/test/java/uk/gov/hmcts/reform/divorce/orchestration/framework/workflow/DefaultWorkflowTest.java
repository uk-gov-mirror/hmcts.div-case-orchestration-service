package uk.gov.hmcts.reform.divorce.orchestration.framework.workflow;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.DefaultTaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.Task;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskException;

import java.util.Map;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

@RunWith(SpringRunner.class)
public class DefaultWorkflowTest {

    private DefaultWorkflow<String> defaultWorkflow;
    private String payload;

    @Before
    public void setup() {
        defaultWorkflow = new DefaultWorkflow<>();
        payload = "";
    }

    @Test
    public void executeShouldReturnTheModifiedPayloadAfterRunningTasks() throws Exception {
        Task<String> taskOne = (context, payload) -> payload.concat("1");
        Task<String> taskTwo = (context, payload) -> payload.concat("2");
        Task<String> taskThree = (context, payload) -> payload.concat("3");

        Task[] tasks = new Task[] {
            taskOne, taskTwo, taskThree
        };

        assertEquals("123", defaultWorkflow.execute(tasks, payload));
        assertThat(defaultWorkflow.getContext(), is(nullValue()));
    }

    @Test
    public void executeShouldStopAfterContextStatusIsSetToFailed() throws Exception {
        Task<String> taskOne = (context, payload) -> payload.concat("1");
        Task<String> taskTwo = (context, payload) -> {
            context.setTaskFailed(true);
            return payload.concat("2");
        };
        Task<String> taskThree = (context, payload) -> payload.concat("3");

        Task[] tasks = new Task[] {
            taskOne, taskTwo, taskThree
        };

        assertEquals("12", defaultWorkflow.execute(tasks, payload));
    }

    @Test
    public void executeShouldStopIfContextStatusIsSetToFailed() throws Exception {
        Task<String> taskOne = (context, payload) -> payload.concat("1");
        Task<String> taskTwo = (context, payload) -> payload.concat("2");
        Task<String> taskThree = (context, payload) -> payload.concat("3");

        Task[] tasks = new Task[] {
            taskOne, taskTwo, taskThree
        };
        DefaultTaskContext context = new DefaultTaskContext();
        context.setTaskFailed(true);
        assertEquals(payload, defaultWorkflow.execute(tasks, context, payload));
    }

    @Test
    public void executeWillSetOptionalPairsIntoTheContext() throws Exception {
        Task<String> task = (context, payload) -> context.getTransientObject("testKey").toString();

        Task[] tasks = new Task[] {task};

        Pair pair = new ImmutablePair<>("testKey", "testValue");

        assertEquals("testValue", defaultWorkflow.execute(tasks, payload, pair));
    }

    @Test(expected = NullPointerException.class)
    public void executeShouldThrowExceptionWithNoTasks() throws Exception {
        defaultWorkflow.execute(null, payload);
    }

    @Test(expected = WorkflowException.class)
    public void executeShouldThrowExceptionWhenATaskExceptionIsThrown_AndNoErrorsWereSetInContext() throws Exception {
        Task[] tasks = new Task[] {(context, payload) -> {
            throw new TaskException("Error");
        }
        };
        defaultWorkflow.execute(tasks, payload);
    }

    @Test
    public void executeShouldThrowExceptionWithErrors_WhenATaskExceptionIsThrown_AndErrorsWereSetInContext() {
        TaskException testTaskException = new TaskException("Error");
        Task[] tasks = new Task[] {(context, payload) -> {
            context.setTransientObject("Test_Error", "this error was set into the context");
            throw testTaskException;
        }
        };
        WorkflowExceptionWithErrors workflowExceptionWithErrors =
            assertThrows(WorkflowExceptionWithErrors.class, () -> defaultWorkflow.execute(tasks, payload));

        assertThat(workflowExceptionWithErrors.getMessage(), is("Workflow execution has generated errors"));
        assertThat(workflowExceptionWithErrors.errors().get("Test_Error"), is("this error was set into the context"));
        Throwable primaryCause = workflowExceptionWithErrors.getCause();
        assertThat(primaryCause, is(instanceOf(WorkflowException.class)));
        Throwable secondaryCause = primaryCause.getCause();
        assertThat(secondaryCause, is(testTaskException));
    }

    @Test
    public void errorsShouldReturnEmptyListWhenNoErrorsAreInContext() throws Exception {
        Task<String> taskOne = (context, payload) -> {
            context.setTransientObject("hello", "world");
            return payload;
        };

        Task[] tasks = new Task[] {
            taskOne
        };

        String returnedPayload = defaultWorkflow.execute(tasks, payload);
        assertThat(returnedPayload, is(payload));
        assertThat(defaultWorkflow.getContext(), is(nullValue()));
    }

    @Test
    public void errorsShouldReturnListOfErrorsWhenErrorsAreInContext() {
        Task<String> taskOne = (context, payload) -> {
            context.setTransientObject("one_Error", "error");
            return payload;
        };
        Task<String> taskTwo = (context, payload) -> {
            context.setTransientObject("two_Error", "error");
            return payload;
        };

        Task[] tasks = new Task[] {
            taskOne, taskTwo
        };

        WorkflowExceptionWithErrors workflowException =
            assertThrows(WorkflowExceptionWithErrors.class, () -> defaultWorkflow.execute(tasks, payload));

        assertThat(workflowException.getMessage(), is("Workflow execution has generated errors"));
        Map<String, Object> errors = workflowException.errors();
        assertThat(errors.size(), is(2));
        assertThat(errors, allOf(
            hasEntry("one_Error", "error"),
            hasEntry("two_Error", "error")
        ));
        assertThat(defaultWorkflow.getContext(), is(nullValue()));
    }

}