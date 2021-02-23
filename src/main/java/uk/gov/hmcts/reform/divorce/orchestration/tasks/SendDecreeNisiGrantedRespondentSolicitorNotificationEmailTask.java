package uk.gov.hmcts.reform.divorce.orchestration.tasks;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.email.EmailTemplateNames;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.TaskContext;
import uk.gov.hmcts.reform.divorce.orchestration.framework.workflow.task.generics.RespondentSolicitorSendEmailTask;
import uk.gov.hmcts.reform.divorce.orchestration.service.EmailService;

import java.util.Map;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.email.EmailTemplateNames.SOL_RESPONDENT_DECREE_NISI_GRANTED;
import static uk.gov.hmcts.reform.divorce.orchestration.service.bulk.print.dataextractor.FullNamesDataExtractor.getPetitionerFullName;
import static uk.gov.hmcts.reform.divorce.orchestration.service.bulk.print.dataextractor.FullNamesDataExtractor.getRespondentFullName;

@Component
public class SendDecreeNisiGrantedRespondentSolicitorNotificationEmailTask extends RespondentSolicitorSendEmailTask {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class EmailMetadata {
        public static final String EMAIL_SUBJECT = "Decree Nisi granted - Solicitor (Respondent)";
        public static final EmailTemplateNames TEMPLATE_ID = SOL_RESPONDENT_DECREE_NISI_GRANTED;
    }

    protected SendDecreeNisiGrantedRespondentSolicitorNotificationEmailTask(EmailService emailService) {
        super(emailService);
    }

    @Override
    protected String getSubject(TaskContext context, Map<String, Object> caseData) {
        return format(
            "%s vs %s: %s",
            getPetitionerFullName(caseData),
            getRespondentFullName(caseData),
            EmailMetadata.EMAIL_SUBJECT
        );
    }

    @Override
    protected EmailTemplateNames getTemplate() {
        return EmailMetadata.TEMPLATE_ID;
    }
}