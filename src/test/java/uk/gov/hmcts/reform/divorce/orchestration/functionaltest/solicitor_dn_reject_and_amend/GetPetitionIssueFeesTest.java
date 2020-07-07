package uk.gov.hmcts.reform.divorce.orchestration.functionaltest.solicitor_dn_reject_and_amend;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CcdCallbackResponse;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.fees.FeeResponse;

import java.util.Collections;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_FEE_AMOUNT;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_FEE_CODE;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_FEE_DESCRIPTION;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_FEE_VERSION;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.convertObjectToJsonString;

public class GetPetitionIssueFeesTest extends GetPetitionIssueFeesAbstractTest {

    @Test
    public void givenCaseData_whenAddingCaseRoleFails_thenReturnErrorResponse() throws Exception {
        FeeResponse feeResponse = FeeResponse.builder()
            .amount(TEST_FEE_AMOUNT)
            .feeCode(TEST_FEE_CODE)
            .version(TEST_FEE_VERSION)
            .description(TEST_FEE_DESCRIPTION)
            .build();

        stubGetFeeFromFeesAndPayments(feeResponse, false);
        stubMaintenanceServerEndpointForAddPetitionerSolicitorRole(HttpStatus.BAD_GATEWAY);

        CcdCallbackResponse expected = CcdCallbackResponse.builder()
            .errors(Collections.singletonList("Problem setting the [PETSOLICITOR] role to the case"))
            .build();

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(callbackRequest))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(expected)));
    }

    @Test
    public void givenUnauthorizedRequest_whenGetPetitionIssueFees_thenReturnErrorData() throws Exception {
        FeeResponse feeResponse = FeeResponse.builder()
            .amount(TEST_FEE_AMOUNT)
            .feeCode(TEST_FEE_CODE)
            .version(TEST_FEE_VERSION)
            .description(TEST_FEE_DESCRIPTION)
            .build();

        stubMaintenanceServerEndpointForAddPetitionerSolicitorRole(HttpStatus.FORBIDDEN);
        stubGetFeeFromFeesAndPayments(feeResponse, false);

        CcdCallbackResponse expected = CcdCallbackResponse.builder()
            .errors(Collections.singletonList("Problem setting the [PETSOLICITOR] role to the case"))
            .build();

        webClient.perform(post(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .content(convertObjectToJsonString(callbackRequest))
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().json(convertObjectToJsonString(expected)));
    }

}