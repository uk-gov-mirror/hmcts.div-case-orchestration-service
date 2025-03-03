package uk.gov.hmcts.reform.divorce.orchestration.tasks;

import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.divorce.model.documentupdate.GeneratedDocumentInfo;
import uk.gov.hmcts.reform.divorce.orchestration.client.DocumentGeneratorClient;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.document.template.DocumentType;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.document.template.DocumentTypeHelper;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.documentgeneration.GenerateDocumentRequest;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.Task;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskContext;

import java.util.LinkedHashSet;
import java.util.Map;

import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.ACCESS_CODE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.AUTH_TOKEN_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CASE_DETAILS_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_CASE_DETAILS_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_COLLECTION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_TYPE_RESPONDENT_INVITATION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.RESPONDENT_INVITATION_FILE_NAME_FORMAT;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.RESPONDENT_PIN;

@RequiredArgsConstructor
@Component
public class RespondentAosLetterGeneratorTask implements Task<Map<String, Object>> {
    private final DocumentGeneratorClient documentGeneratorClient;

    @Override
    public Map<String, Object> execute(TaskContext context, Map<String, Object> caseData) {
        CaseDetails caseDetails = context.getTransientObject(CASE_DETAILS_JSON_KEY);
        String templateId = DocumentTypeHelper.getLanguageAppropriateTemplate(caseData, DocumentType.AOS_INVITATION_REP_RESP);

        GeneratedDocumentInfo aosInvitation =
            documentGeneratorClient.generatePDF(
                GenerateDocumentRequest.builder()
                    .template(templateId)
                    .values(ImmutableMap.of(
                        DOCUMENT_CASE_DETAILS_JSON_KEY, caseDetails,
                        ACCESS_CODE, context.getTransientObject(RESPONDENT_PIN))
                    )
                    .build(),
                context.getTransientObject(AUTH_TOKEN_JSON_KEY)
            );

        aosInvitation.setDocumentType(DOCUMENT_TYPE_RESPONDENT_INVITATION);
        aosInvitation.setFileName(String.format(RESPONDENT_INVITATION_FILE_NAME_FORMAT,
            caseDetails.getCaseId()));

        final LinkedHashSet<GeneratedDocumentInfo> documentCollection = context.computeTransientObjectIfAbsent(DOCUMENT_COLLECTION,
            new LinkedHashSet<>());
        documentCollection.add(aosInvitation);

        return caseData;
    }
}
