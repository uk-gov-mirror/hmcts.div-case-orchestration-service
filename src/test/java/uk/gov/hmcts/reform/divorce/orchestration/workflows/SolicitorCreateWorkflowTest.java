package uk.gov.hmcts.reform.divorce.orchestration.workflows;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.Features;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.divorce.orchestration.service.FeatureToggleService;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.AddMiniPetitionDraftTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.AddNewDocumentsToCaseDataTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.CopyD8JurisdictionConnectionPolicyTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.RespondentOrganisationPolicyRemovalTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SetClaimCostsFromTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SetNewLegalConnectionPolicyTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SetPetitionerSolicitorOrganisationPolicyReferenceTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SetRespondentSolicitorOrganisationPolicyReferenceTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.SetSolicitorCourtDetailsTask;
import uk.gov.hmcts.reform.divorce.orchestration.tasks.ValidateSelectedOrganisationTask;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DIVORCE_COSTS_CLAIM_CCD_FIELD;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.YES_VALUE;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.CaseDataTestHelper.buildOrganisationPolicy;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.Verificators.mockTasksExecution;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.Verificators.verifyTasksCalledInOrder;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.Verificators.verifyTasksWereNeverCalled;

@RunWith(MockitoJUnitRunner.class)
public class SolicitorCreateWorkflowTest {

    @Mock
    FeatureToggleService featureToggleService;

    @Mock
    AddMiniPetitionDraftTask addMiniPetitionDraftTask;

    @Mock
    AddNewDocumentsToCaseDataTask addNewDocumentsToCaseDataTask;

    @Mock
    SetSolicitorCourtDetailsTask setSolicitorCourtDetailsTask;

    @Mock
    SetClaimCostsFromTask setClaimCostsFromTask;

    @Mock
    SetPetitionerSolicitorOrganisationPolicyReferenceTask setPetitionerSolicitorOrganisationPolicyReferenceTask;

    @Mock
    SetRespondentSolicitorOrganisationPolicyReferenceTask setRespondentSolicitorOrganisationPolicyReferenceTask;

    @Mock
    SetNewLegalConnectionPolicyTask setNewLegalConnectionPolicyTask;

    @Mock
    CopyD8JurisdictionConnectionPolicyTask copyD8JurisdictionConnectionPolicyTask;

    @Mock
    RespondentOrganisationPolicyRemovalTask respondentOrganisationPolicyRemovalTask;

    @Mock
    private ValidateSelectedOrganisationTask validateSelectedOrganisationTask;

    @InjectMocks
    SolicitorCreateWorkflow solicitorCreateWorkflow;

    @Test
    public void runShouldSetClaimCostsFromWhenClaimCostsIsYesAndClaimCostsFromIsEmpty() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put(DIVORCE_COSTS_CLAIM_CCD_FIELD, YES_VALUE);

        when(featureToggleService.isFeatureEnabled(Features.REPRESENTED_RESPONDENT_JOURNEY)).thenReturn(true);

        CaseDetails caseDetails = CaseDetails.builder().caseData(payload).build();

        mockTasksExecution(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask,
            setPetitionerSolicitorOrganisationPolicyReferenceTask,
            respondentOrganisationPolicyRemovalTask
        );

        assertThat(solicitorCreateWorkflow.run(caseDetails, AUTH_TOKEN), is(caseDetails.getCaseData()));

        verifyTasksCalledInOrder(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask,
            setPetitionerSolicitorOrganisationPolicyReferenceTask,
            respondentOrganisationPolicyRemovalTask
        );

        verifyTasksWereNeverCalled(setRespondentSolicitorOrganisationPolicyReferenceTask);
    }

    @Test
    public void shouldExecuteSetRespondentSolicitorOrganisationPolicyReferenceTaskWhenFtOnAndRespSolDigital()
        throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put(DIVORCE_COSTS_CLAIM_CCD_FIELD, YES_VALUE);
        payload.put(CcdFields.RESPONDENT_SOLICITOR_DIGITAL, YES_VALUE);
        payload.put(CcdFields.RESPONDENT_SOLICITOR_ORGANISATION_POLICY, buildOrganisationPolicy());

        CaseDetails caseDetails = CaseDetails.builder().caseData(payload).build();

        when(featureToggleService.isFeatureEnabled(Features.REPRESENTED_RESPONDENT_JOURNEY)).thenReturn(true);
        when(featureToggleService.isFeatureEnabled(Features.SHARE_A_CASE)).thenReturn(true);

        mockTasksExecution(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask,
            validateSelectedOrganisationTask,
            setPetitionerSolicitorOrganisationPolicyReferenceTask,
            setRespondentSolicitorOrganisationPolicyReferenceTask
        );

        assertThat(solicitorCreateWorkflow.run(caseDetails, AUTH_TOKEN), is(caseDetails.getCaseData()));

        verifyTasksCalledInOrder(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask,
            validateSelectedOrganisationTask,
            setPetitionerSolicitorOrganisationPolicyReferenceTask,
            setRespondentSolicitorOrganisationPolicyReferenceTask
        );

        verifyTasksWereNeverCalled(respondentOrganisationPolicyRemovalTask);
    }

    @Test
    public void runShouldNotRunSetPetitionerSolicitorOrganisationPolicyReferenceDetailTaskWhenFeatureIsOff() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put(DIVORCE_COSTS_CLAIM_CCD_FIELD, YES_VALUE);

        when(featureToggleService.isFeatureEnabled(Features.REPRESENTED_RESPONDENT_JOURNEY)).thenReturn(false);

        CaseDetails caseDetails = CaseDetails.builder().caseData(payload).build();

        mockTasksExecution(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask
        );

        assertThat(solicitorCreateWorkflow.run(caseDetails, AUTH_TOKEN), is(caseDetails.getCaseData()));

        verifyTasksCalledInOrder(
            caseDetails.getCaseData(),
            setClaimCostsFromTask,
            setSolicitorCourtDetailsTask,
            setNewLegalConnectionPolicyTask,
            copyD8JurisdictionConnectionPolicyTask,
            addMiniPetitionDraftTask,
            addNewDocumentsToCaseDataTask
        );

        verifyTasksWereNeverCalled(
            validateSelectedOrganisationTask,
            setPetitionerSolicitorOrganisationPolicyReferenceTask,
            setRespondentSolicitorOrganisationPolicyReferenceTask,
            respondentOrganisationPolicyRemovalTask
        );
    }
}
