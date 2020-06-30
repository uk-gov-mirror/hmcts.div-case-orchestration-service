package uk.gov.hmcts.reform.divorce.orchestration.functionaltest;

import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;

import java.util.Collections;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

public class HealthCheckITest extends MockedFunctionalTest {

    private static final String HEALTH_UP_RESPONSE = "{ \"status\": \"UP\"}";
    private static final String HEALTH_DOWN_RESPONSE = "{ \"status\": \"DOWN\"}";

    @LocalServerPort
    private int port;

    private String healthUrl;
    private final HttpClient httpClient = HttpClients.createMinimal();

    private HttpResponse getHealth() throws Exception {
        final HttpGet request = new HttpGet(healthUrl);
        request.addHeader("Accept", "application/json;charset=UTF-8");

        return httpClient.execute(request);
    }

    @Before
    public void setUp() {
        healthUrl = "http://localhost:" + port + "/health";
    }

    @Autowired
    private AuthTokenGenerator authTokenGenerator;//TODO - This is caching response - write a quick test to prove it

    @Autowired
    private ServiceAuthorisationApi serviceAuthorisationApi;

    @Test
    public void throwAwayTest() {
        String peter = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IlBldGVyIEdyaWZmaW4iLCJpYXQiOjE1MTYyMzkwMjJ9.kuCi5JqA3Z7g8pBAv2_lw3kBW8SHb1Ipdn2qpNqST88";
        stubServiceAuthProvider(HttpStatus.OK, peter);
        System.out.println("hey: " + authTokenGenerator.generate());
        assertThat(authTokenGenerator.generate(), is("Bearer " + peter));
//        String body = new RestTemplate().postForEntity("http://localhost:4504/lease", null, String.class).getBody();//TODO - comment this if it doesn't work
        String feignBody = serviceAuthorisationApi.serviceToken(Collections.emptyMap());
        resetMock();
//        serviceAuthProviderServer.resetAll();

        System.out.println("ho: " + authTokenGenerator.generate());

        String lois = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkxvaXMgR3JpZmZpbiIsImlhdCI6MTUxNjIzOTAyMn0.Y4JewzwRUNJ2FG-nGpgcEpDbHAgu9RvqlT5_GcT3jhc";
        stubServiceAuthProvider(HttpStatus.OK, lois);
        System.out.println("let's go: " + authTokenGenerator.generate());
        String body2 = new RestTemplate().postForEntity("http://localhost:4504/lease", null, String.class).getBody();//TODO - confirmed. it's not the mock. it's the bean
        String feignBody2 = serviceAuthorisationApi.serviceToken(Collections.emptyMap());
        assertThat(authTokenGenerator.generate(), is("Bearer " + lois));
        resetMock();
//        serviceAuthProviderServer.resetAll();
        //TODO - carry on
    }

    @Test
    public void givenAllDependenciesAreUp_whenCheckHealth_thenReturnStatusUp() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));

        assertThat(JsonPath.read(body, "$.details.sendLetterServiceHealthCheck.status").toString(), equalTo("UP"));
    }


    @Test
    public void givenSendLetterServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, false);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, false);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.sendLetterServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenCaseFormatterServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, false);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenDocumentGeneratorServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, false);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenCaseMaintenanceServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, false);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenFeesAndPaymentsServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, false);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenPaymentServiceIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, false);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, true);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenServiceAuthIsDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, true);
        mockEndpointAndResponse(maintenanceServiceServer, true);
        mockEndpointAndResponse(validationServiceServer, true);
        mockEndpointAndResponse(documentGeneratorServiceServer, true);
        mockEndpointAndResponse(feesAndPaymentsServer, true);
        mockEndpointAndResponse(idamServer, true);
        mockEndpointAndResponse(paymentServiceServer, true);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, false);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("UP"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    @Test
    public void givenAllDependenciesAreDown_whenCheckHealth_thenReturnStatusDown() throws Exception {
        mockEndpointAndResponse(formatterServiceServer, false);
        mockEndpointAndResponse(maintenanceServiceServer, false);
        mockEndpointAndResponse(validationServiceServer, false);
        mockEndpointAndResponse(documentGeneratorServiceServer, false);
        mockEndpointAndResponse(feesAndPaymentsServer, false);
        mockEndpointAndResponse(idamServer, false);
        mockEndpointAndResponse(paymentServiceServer, false);
        mockEndpointAndResponse(sendLetterService, true);
//        mockEndpointAndResponse(serviceAuthProviderServer, false);

        HttpResponse response = getHealth();
        String body = EntityUtils.toString(response.getEntity());

        assertThat(response.getStatusLine().getStatusCode(), equalTo(503));
        assertThat(JsonPath.read(body, "$.status").toString(), equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseFormatterServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.caseMaintenanceServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.documentGeneratorServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.feesAndPaymentsServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.paymentServiceHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.serviceAuthProviderHealthCheck.status").toString(),
            equalTo("DOWN"));
        assertThat(JsonPath.read(body, "$.details.diskSpace.status").toString(), equalTo("UP"));
    }

    private void mockEndpointAndResponse(WireMockClassRule mockServer, boolean serviceUp) {
        mockServer.stubFor(get(urlEqualTo("/health"))
            .willReturn(aResponse()
                .withStatus(serviceUp ? HttpStatus.OK.value() : HttpStatus.SERVICE_UNAVAILABLE.value())
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
                .withBody(serviceUp ? HEALTH_UP_RESPONSE : HEALTH_DOWN_RESPONSE)));
    }
}
