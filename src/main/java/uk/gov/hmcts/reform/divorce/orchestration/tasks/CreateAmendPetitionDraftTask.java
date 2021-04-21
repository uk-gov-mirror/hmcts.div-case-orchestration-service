package uk.gov.hmcts.reform.divorce.orchestration.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.divorce.orchestration.client.CaseMaintenanceClient;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.Task;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskException;

import java.util.HashMap;
import java.util.Map;

import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AUTH_TOKEN_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CASE_ID_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.NEW_AMENDED_PETITION_DRAFT_KEY;

@Component
@RequiredArgsConstructor
public class CreateAmendPetitionDraftTask implements Task<Map<String, Object>> {

    private final CaseMaintenanceClient caseMaintenanceClient;

    @Override
    public Map<String, Object> execute(TaskContext context,
                                       Map<String, Object> draft) {

        final Map<String, Object> amendDraft = caseMaintenanceClient
            .amendPetition(context.getTransientObject(AUTH_TOKEN_JSON_KEY).toString());

        if(amendDraft != null){
            ObjectMapper mapper = new ObjectMapper();
            ResponseEntity response = mapper.convertValue(
                amendDraft,
                ResponseEntity.class);

            if( response.getStatusCode() != HttpStatus.OK) {
                throw new TaskException("Amend draft creation failed for case {}", context.getTransientObject(CASE_ID_JSON_KEY));
            }

            context.setTransientObject(NEW_AMENDED_PETITION_DRAFT_KEY, amendDraft);
        }

        // return empty as next step (update case state AmendPetition) needs no data (empty)
        return new HashMap<>();
    }
}
