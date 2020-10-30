package uk.gov.hmcts.reform.divorce.orchestration.functionaltest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.jayway.jsonpath.JsonPath;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONString;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.divorce.model.ccd.CoreCaseData;
import uk.gov.hmcts.reform.divorce.model.usersession.DivorceSession;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.CaseDataResponse;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.ccd.CaseDetails;
import uk.gov.hmcts.reform.divorce.orchestration.domain.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.orchestration.testutil.CourtsMatcher;
import uk.gov.hmcts.reform.divorce.orchestration.testutil.JSONComparisonMatcher;
import uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.AUTH_TOKEN;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_CASE_ID;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_COURT;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_EMAIL;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_ERROR;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_STATE;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.DataTransformationTestHelper.getExpectedTranslatedDivorceSessionData;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.DataTransformationTestHelper.getExpectedTranslatedDivorceSessionJsonAsMap;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.DataTransformationTestHelper.getTestCoreCaseData;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.convertObjectToJsonString;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.getJsonFromResourceFile;
import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.getObjectMapperInstance;

public class RetrieveCaseITest extends IdamTestSupport {

    private static final String API_URL = "/retrieve-case";
    private static final String GET_CASE_CONTEXT_PATH = "/casemaintenance/version/1/case";

    private CaseDetails caseDetails;

    private CoreCaseData testCcdData;
    private DivorceSession expectedTranslatedDivorceSessionData;

    @Autowired
    private MockMvc webClient;

    @Before
    public void setUp() throws IOException {
        stubUserDetailsEndpoint(OK, AUTH_TOKEN, convertObjectToJsonString(UserDetails.builder().email(TEST_EMAIL).build()));
        testCcdData = getTestCoreCaseData();
        expectedTranslatedDivorceSessionData = getExpectedTranslatedDivorceSessionData();//TODO - do I need this method?

        Map<String, Object> testCcdDataMap = getObjectMapperInstance().readValue(convertObjectToJsonString(testCcdData), new TypeReference<Map<String, Object>>() {
        });//TODO - get it passing, then refactor

        caseDetails =
            CaseDetails.builder()
                .caseId(TEST_CASE_ID)
                .state(TEST_STATE)
                .caseData(testCcdDataMap)
                .build();
    }

    @Test
    public void givenNoAuthToken_whenRetrieveCase_thenReturnBadRequest() throws Exception {
        webClient.perform(get(API_URL)
            .accept(APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void givenCMSThrowsException_whenRetrieveAosCase_thenPropagateException() throws Exception {
        stubGetCaseFromCMS(HttpStatus.INTERNAL_SERVER_ERROR, TEST_ERROR);

        webClient.perform(get(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .accept(APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString(TEST_ERROR)));
    }

    @Test
    public void givenNoCaseExists_whenGetCase_thenReturnEmptyResponse() throws Exception {
        stubGetCaseFromCMS(null);

        webClient.perform(get(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .accept(APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void givenCFSThrowsException_whenGetCase_thenPropagateException() throws Exception {
        stubGetCaseFromCMS(caseDetails);

//        stubFormatterServerEndpoint(HttpStatus.INTERNAL_SERVER_ERROR, TEST_ERROR);//TODO - how do we solve this?

        webClient.perform(get(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .accept(APPLICATION_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(content().string(containsString(TEST_ERROR)));
    }

    @Test
    public void givenMultipleCases_whenGetCase_thenPropagateException() throws Exception {
        stubGetMultipleCaseFromCMS();

        webClient.perform(get(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .accept(APPLICATION_JSON))
            .andExpect(status().isMultipleChoices())
            .andExpect(content().string(""));
    }

    @Test
    public void givenAllGoesWellProceedAsExpected_RetrieveCaseInformation() throws Exception {
        stubGetCaseFromCMS(caseDetails);

//        Map<String, Object> expectedTranslatedDivorceSessionData = getExpectedTranslatedDivorceSessionJsonAsMap();//TODO - get it passing, then refactor
        JsonNode expectedTranslatedDivorceSessionData = getJsonFromResourceFile("/jsonExamples/payloads/transformations/ccdtodivorce/divorce/case-data.json", JsonNode.class);//TODO - get it passing, then refactor
        //TODO - put this in separate class

//        CaseDataResponse expectedCaseDataResponse = CaseDataResponse.builder()
//            .data(expectedTranslatedDivorceSessionData)
//            .caseId(TEST_CASE_ID)
//            .state(TEST_STATE)
//            .court(TEST_COURT)
//            .build();

        String responseBody = webClient.perform(get(API_URL)
            .header(AUTHORIZATION, AUTH_TOKEN)
            .accept(APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(responseBody, isJson());
        assertThat(responseBody, hasJsonPath("$.caseId", is(TEST_CASE_ID)));
        assertThat(responseBody, hasJsonPath("$.state", is(TEST_STATE)));
        assertThat(responseBody, hasJsonPath("$.courts", is(TEST_COURT)));
        assertThat(responseBody, hasJsonPath("$.data.court", CourtsMatcher.isExpectedCourtsList()));//TODO - is this still needed?
        String actualCaseData = getObjectMapperInstance().readTree(responseBody).get("data").toString();
//        assertThat(responseBody, hasJsonPath("$.data", is(new JSONComparisonMatcher(convertObjectToJsonString(expectedTranslatedDivorceSessionData)))));

        JSONAssert.assertEquals(expectedTranslatedDivorceSessionData.toPrettyString(), actualCaseData, false);//TODO - try strict when this works?
//            .andExpect(content().json(convertObjectToJsonString(expectedCaseDataResponse)))//TODO - might be better  to write the json matchers
    }

    private void stubGetCaseFromCMS(CaseDetails caseDetails) {
        stubGetCaseFromCMS(OK, convertObjectToJsonString(caseDetails));
    }

    private void stubGetCaseFromCMS(HttpStatus status, String message) {
        maintenanceServiceServer.stubFor(WireMock.get(GET_CASE_CONTEXT_PATH)
            .withHeader(AUTHORIZATION, new EqualToPattern(AUTH_TOKEN))
            .willReturn(aResponse()
                .withStatus(status.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .withBody(message)));
    }

    private void stubGetMultipleCaseFromCMS() {
        stubGetCaseFromCMS(HttpStatus.MULTIPLE_CHOICES, "");
    }

}