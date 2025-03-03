package uk.gov.hmcts.reform.divorce.orchestration.functionaltest;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.divorce.model.response.ValidationResponse;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackRequest;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackResponse;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.courts.CourtEnum;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.fees.FeeResponse;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.idam.Pin;
import uk.gov.hmcts.reform.divorce.utils.DateUtils;
import uk.gov.hmcts.reform.divorce.validation.service.ValidationService;
import uk.gov.hmcts.reform.idam.client.models.GeneratePinRequest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.BEARER_AUTH_TOKEN_1;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_LETTER_HOLDER_ID_CODE;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_PIN_CODE;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_USER_FIRST_NAME;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_USER_LAST_NAME;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.ACCESS_CODE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.CO_RESPONDENT_LETTER_HOLDER_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_CASE_DETAILS_JSON_KEY;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_TYPE_CO_RESPONDENT_INVITATION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_TYPE_PETITION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.DOCUMENT_TYPE_RESPONDENT_INVITATION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_CO_RESPONDENT_NAMED;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_DIVORCE_UNIT;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_PETITIONER_FIRST_NAME;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_PETITIONER_LAST_NAME;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.D_8_REASON_FOR_DIVORCE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.GENERATE_AOS_INVITATION;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.ISSUE_DATE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.PETITION_ISSUE_FEE_FOR_LETTER;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.RESPONDENT_LETTER_HOLDER_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.facts.DivorceFact.ADULTERY;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.convertObjectToJsonString;

@SpringBootTest(properties = {"feature-toggle.toggle.represented_respondent_journey=false"})
public class PetitionIssuedITest extends IdamTestSupport {

    private static final String API_URL = "/petition-issued";
    private static final String PETITION_ISSUE_FEE_CONTEXT_PATH = "/fees-and-payments/version/1/petition-issue-fee";
    private static final String MINI_PETITION_TEMPLATE_NAME = "divorceminipetition";
    private static final String RESPONDENT_INVITATION_TEMPLATE_NAME = "aosinvitation";
    public static final String CO_RESPONDENT_INVITATION_TEMPLATE_NAME = "co-respondentinvitation";

    private static final Map<String, Object> CASE_DATA = new HashMap<>();

    private static final CaseDetails CASE_DETAILS = CaseDetails.builder()
        .caseData(CASE_DATA)
        .caseId(TEST_CASE_ID)
        .build();

    private static final CcdCallbackRequest CREATE_EVENT = CcdCallbackRequest.builder()
        .caseDetails(CASE_DETAILS)
        .build();

    private static final ValidationResponse VALIDATION_RESPONSE = ValidationResponse.builder().build();

    @Autowired
    private MockMvc webClient;

    @MockBean
    private ValidationService validationService;

    @BeforeClass
    public static void beforeClass() {
        CASE_DATA.put(D_8_PETITIONER_FIRST_NAME, TEST_USER_FIRST_NAME);
        CASE_DATA.put(D_8_PETITIONER_LAST_NAME, TEST_USER_LAST_NAME);
        CASE_DATA.put(RESPONDENT_LETTER_HOLDER_ID, TEST_LETTER_HOLDER_ID_CODE);
        CASE_DATA.put(CO_RESPONDENT_LETTER_HOLDER_ID, TEST_LETTER_HOLDER_ID_CODE);
        CASE_DATA.put(ISSUE_DATE, LocalDate.now().format(DateUtils.Formatters.CCD_DATE));
    }

    @Test
    public void givenCallbackRequestIsNull_whenPetitionIssued_thenReturnBadRequest() throws Exception {
        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenJWTTokenIsNull_whenPetitionIssued_thenReturnBadRequest() throws Exception {
        webClient.perform(post(API_URL)
            .content(convertObjectToJsonString(CREATE_EVENT))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenValidationFailed_whenPetitionIssued_thenReturnCaseWithValidationErrors() throws Exception {
        final List<String> errors = getListOfErrors();
        final ValidationResponse validationResponseFail = ValidationResponse.builder()
            .errors(errors)
            .warnings(Collections.singletonList("Warning!"))
            .build();
        when(validationService.validate(any())).thenReturn(validationResponseFail);

        final CcdCallbackResponse ccdCallbackResponse =
            CcdCallbackResponse.builder()
                .errors(errors)
                .build();

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(CREATE_EVENT))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(ccdCallbackResponse)));
    }

    @Test
    public void givenGenerateAOSInvitationIsNull_whenPetitionIssued_thenReturnCaseExpectedChanges() throws Exception {

        final Map<String, Object> formattedCaseData = emptyMap();

        stubDocumentGeneratorService(MINI_PETITION_TEMPLATE_NAME,
            singletonMap(DOCUMENT_CASE_DETAILS_JSON_KEY, CASE_DETAILS), DOCUMENT_TYPE_PETITION);
        when(validationService.validate(any())).thenReturn(VALIDATION_RESPONSE);

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(CREATE_EVENT))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(formattedCaseData)));
    }

    @Test
    public void givenGenerateAOSInvitationIsFalse_whenPetitionIssued_thenReturnCaseExpectedChanges() throws Exception {
        final Map<String, Object> formattedCaseData = emptyMap();

        stubDocumentGeneratorService(MINI_PETITION_TEMPLATE_NAME,
            singletonMap(DOCUMENT_CASE_DETAILS_JSON_KEY, CASE_DETAILS), DOCUMENT_TYPE_PETITION);
        when(validationService.validate(any())).thenReturn(VALIDATION_RESPONSE);

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(CREATE_EVENT))
            .param(GENERATE_AOS_INVITATION, "false")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(formattedCaseData)));
    }

    @Test
    public void givenGenerateInvitationIsTrue_whenPetitionIssued_thenReturnCaseExpectedChanges() throws Exception {
        final GeneratePinRequest pinRequest =
            GeneratePinRequest.builder()
                .firstName(TEST_USER_FIRST_NAME)
                .lastName(TEST_USER_LAST_NAME)
                .build();

        final Pin pin = Pin.builder().pin(TEST_PIN_CODE).userId(TEST_LETTER_HOLDER_ID_CODE).build();

        final Map<String, Object> formattedCaseData = emptyMap();

        stubSignIn();
        stubPinDetailsEndpoint(BEARER_AUTH_TOKEN_1, pinRequest, pin);
        stubDocumentGeneratorService(MINI_PETITION_TEMPLATE_NAME,
            singletonMap(DOCUMENT_CASE_DETAILS_JSON_KEY, CASE_DETAILS), DOCUMENT_TYPE_PETITION);
        when(validationService.validate(any())).thenReturn(VALIDATION_RESPONSE);

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(CREATE_EVENT))
            .param(GENERATE_AOS_INVITATION, "true")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(formattedCaseData)));
    }

    @Test
    public void givenGenerateInvitationIsTrueAndIsServiceCentre_whenPetitionIssued_thenReturnCaseExpectedChanges() throws Exception {
        CcdCallbackRequest ccdCallbackRequestWithServiceCentre = CREATE_EVENT;
        ccdCallbackRequestWithServiceCentre.getCaseDetails().getCaseData().put(D_8_DIVORCE_UNIT, CourtEnum.SERVICE_CENTER.getId());

        final GeneratePinRequest pinRequest =
            GeneratePinRequest.builder()
                .firstName(TEST_USER_FIRST_NAME)
                .lastName(TEST_USER_LAST_NAME)
                .build();

        final Pin pin = Pin.builder().pin(TEST_PIN_CODE).userId(TEST_LETTER_HOLDER_ID_CODE).build();

        final Map<String, Object> formattedCaseData = emptyMap();

        stubSignIn();
        stubPinDetailsEndpoint(BEARER_AUTH_TOKEN_1, pinRequest, pin);
        stubDocumentGeneratorService(MINI_PETITION_TEMPLATE_NAME,
            singletonMap(DOCUMENT_CASE_DETAILS_JSON_KEY, ccdCallbackRequestWithServiceCentre.getCaseDetails()),
            DOCUMENT_TYPE_RESPONDENT_INVITATION);
        stubDocumentGeneratorService(RESPONDENT_INVITATION_TEMPLATE_NAME,
            ImmutableMap.of(
                DOCUMENT_CASE_DETAILS_JSON_KEY, ccdCallbackRequestWithServiceCentre.getCaseDetails(),
                ACCESS_CODE, TEST_PIN_CODE
            ),
            DOCUMENT_TYPE_RESPONDENT_INVITATION);
        when(validationService.validate(any())).thenReturn(VALIDATION_RESPONSE);

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(ccdCallbackRequestWithServiceCentre))
            .param(GENERATE_AOS_INVITATION, "true")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(formattedCaseData)));
    }

    @Test
    public void givenGenerateInvitationIsTrueAndIsServiceCentreAndCoRespondentExists_whenPetitionIssued_thenReturnCaseExpectedChanges() throws Exception {
        CcdCallbackRequest ccdCallbackRequestWithServiceCentre = CREATE_EVENT;
        ccdCallbackRequestWithServiceCentre.getCaseDetails().getCaseData().put(D_8_DIVORCE_UNIT,
            CourtEnum.SERVICE_CENTER.getId());
        ccdCallbackRequestWithServiceCentre.getCaseDetails().getCaseData().put(D_8_REASON_FOR_DIVORCE, ADULTERY.getValue());
        ccdCallbackRequestWithServiceCentre.getCaseDetails().getCaseData().put(D_8_CO_RESPONDENT_NAMED, "YES");

        final GeneratePinRequest pinRequest =
            GeneratePinRequest.builder()
                .firstName(TEST_USER_FIRST_NAME)
                .lastName(TEST_USER_LAST_NAME)
                .build();

        final Pin pin = Pin.builder().pin(TEST_PIN_CODE).userId(TEST_LETTER_HOLDER_ID_CODE).build();

        final Map<String, Object> formattedCaseData = emptyMap();

        stubSignIn();
        stubPinDetailsEndpoint(BEARER_AUTH_TOKEN_1, pinRequest, pin);
        stubDocumentGeneratorService(MINI_PETITION_TEMPLATE_NAME, singletonMap(DOCUMENT_CASE_DETAILS_JSON_KEY,
            ccdCallbackRequestWithServiceCentre.getCaseDetails()), DOCUMENT_TYPE_PETITION);
        stubDocumentGeneratorService(RESPONDENT_INVITATION_TEMPLATE_NAME,
            ImmutableMap.of(
                DOCUMENT_CASE_DETAILS_JSON_KEY, ccdCallbackRequestWithServiceCentre.getCaseDetails(),
                ACCESS_CODE, TEST_PIN_CODE),
            DOCUMENT_TYPE_RESPONDENT_INVITATION
        );
        when(validationService.validate(any())).thenReturn(VALIDATION_RESPONSE);

        FeeResponse feeResponse = FeeResponse.builder().amount(550.00).build();

        stubGetFeeFromFeesAndPayments(HttpStatus.OK, feeResponse);

        stubDocumentGeneratorService(CO_RESPONDENT_INVITATION_TEMPLATE_NAME,
            ImmutableMap.of(
                DOCUMENT_CASE_DETAILS_JSON_KEY, ccdCallbackRequestWithServiceCentre.getCaseDetails(),
                ACCESS_CODE, TEST_PIN_CODE,
                PETITION_ISSUE_FEE_FOR_LETTER, "550"),
            DOCUMENT_TYPE_CO_RESPONDENT_INVITATION
        );

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(ccdCallbackRequestWithServiceCentre))
            .param(GENERATE_AOS_INVITATION, "true")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(formattedCaseData)));
    }

    private void stubGetFeeFromFeesAndPayments(HttpStatus status, FeeResponse feeResponse) {
        feesAndPaymentsServer.stubFor(WireMock.get(PETITION_ISSUE_FEE_CONTEXT_PATH)
            .willReturn(aResponse()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withStatus(status.value())
                .withBody(convertObjectToJsonString(feeResponse))));
    }

    private List<String> getListOfErrors() {
        return Arrays.asList(
            "D8InferredPetitionerGender can not be null or empty. Actual data is: null",
            "D8InferredRespondentGender can not be null or empty. Actual data is: null",
            "D8MarriageDate can not be null or empty. Actual data is: null",
            "D8MarriagePetitionerName can not be null or empty. Actual data is: null",
            "D8MarriageRespondentName can not be null or empty. Actual data is: null",
            "D8PetitionerContactDetailsConfidential can not be null or empty. Actual data is: null",
            "D8RespondentFirstName can not be null or empty. Actual data is: null",
            "D8RespondentLastName can not be null or empty. Actual data is: null",
            "D8LegalProceedings can not be null or empty. Actual data is: null",
            "D8ReasonForDivorce can not be null or empty. Actual data is: null",
            "D8FinancialOrder can not be null or empty. Actual data is: null",
            "D8DivorceCostsClaim can not be null or empty. Actual data is: null",
            "D8JurisdictionConnection can not be null or empty. Actual data is: null",
            "D8StatementOfTruth must be 'YES'. Actual data is: null"
        );
    }

}
