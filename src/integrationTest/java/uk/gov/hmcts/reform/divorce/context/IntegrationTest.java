package uk.gov.hmcts.reform.divorce.context;

import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import net.serenitybdd.junit.runners.SerenityRunner;
import net.serenitybdd.junit.spring.integration.SpringIntegrationMethodRule;
import net.serenitybdd.rest.SerenityRest;
import org.assertj.core.util.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.divorce.RetryRule;
import uk.gov.hmcts.reform.divorce.model.idam.UserDetails;
import uk.gov.hmcts.reform.divorce.support.IdamUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;

@Slf4j
@RunWith(SerenityRunner.class)
@ContextConfiguration(classes = {ServiceContextConfiguration.class})
public abstract class IntegrationTest {

    private static final String CASE_WORKER_USERNAME = "TEST_CASE_WORKER_USER";
    private static final String CASE_WORKER_SUPERUSER = "TEST_CASE_WORKER_SUPERUSER";
    private static final String SOLICITOR_USER_NAME = "divorce_as_petitioner_solicitor_02";
    private static final String EMAIL_DOMAIN = "@mailinator.com";
    private static final String CITIZEN_ROLE = "citizen";
    private static final String SOLICITOR_PASSWORD = "Testing1234";
    private static final String PASSWORD = "genericPassword123";
    private static final String CITIZEN_USERGROUP = "citizens";

    protected static final String ERRORS = "errors";
    protected static final String DATA = "data";
    protected static final String CASE_DATA = "case_data";
    protected static final String CASE_DETAILS = "case_details";

    private UserDetails caseWorkerUser;
    private UserDetails caseWorkerSuperUser;

    @Value("${case.orchestration.service.base.uri}")
    protected String serverUrl;

    @Value("${idam.s2s-auth.url}")
    private String idamS2sAuthUrl;

    @Value("${http.proxy:#{null}}")
    protected String httpProxy;

    @Autowired
    protected IdamUtils idamTestSupportUtil;

    @Rule
    public SpringIntegrationMethodRule springMethodIntegration;

    @Rule
    public RetryRule retryRule = new RetryRule(3);

    protected IntegrationTest() {
        this.springMethodIntegration = new SpringIntegrationMethodRule();
    }

    private Stack<List<UserDetails>> listStack = new Stack<>();
    private List<UserDetails> createdUsers;

    @PostConstruct
    public void init() {
        if (!Strings.isNullOrEmpty(httpProxy)) {
            try {
                URL proxy = new URL(httpProxy);
                if (!InetAddress.getByName(proxy.getHost()).isReachable(2000)) { // check proxy connectivity
                    throw new IOException("Could not reach proxy in timeout time");
                }
                System.setProperty("http.proxyHost", proxy.getHost());
                System.setProperty("http.proxyPort", Integer.toString(proxy.getPort()));
                System.setProperty("https.proxyHost", proxy.getHost());
                System.setProperty("https.proxyPort", Integer.toString(proxy.getPort()));
            } catch (IOException e) {
                log.error("Error setting up proxy - are you connected to the VPN?", e);
                throw new RuntimeException("Error setting up proxy", e);
            }
        }
    }

    @Before
    public void initUsers() {
        createdUsers = new ArrayList<>();
        listStack.add(createdUsers);

    }

    @After
    public void onDestroy() {
        List<UserDetails> createdUsersOnTest = listStack.pop();
        createdUsersOnTest.forEach(userDetails -> {
            try {
                deleteUser(userDetails);
            } catch (Exception e) {
                log.error("User deletion failed " + userDetails.getEmailAddress(), e);
            }
        });
    }

    protected UserDetails createCaseWorkerUser() {
        synchronized (this) {
            if (caseWorkerUser == null) {
                caseWorkerUser = wrapInRetry(() -> getCreatedUserDetails(
                    CASE_WORKER_USERNAME +  EMAIL_DOMAIN,
                    PASSWORD));
            }
            return caseWorkerUser;
        }
    }

    protected UserDetails createCaseWorkerSuperUser() {
        synchronized (this) {
            if (caseWorkerSuperUser == null) {
                caseWorkerSuperUser = wrapInRetry(() -> getCreatedUserDetails(
                    CASE_WORKER_SUPERUSER +  EMAIL_DOMAIN,
                    PASSWORD));
            }
            return caseWorkerSuperUser;
        }
    }

    protected UserDetails createCitizenUser() {
        return createCitizenUser(CITIZEN_ROLE);
    }

    protected UserDetails createCitizenUser(String role) {
        return wrapInRetry(() -> {
            final String username = "simulate-delivered" + UUID.randomUUID() + EMAIL_DOMAIN;
            return getUserDetails(username, CITIZEN_USERGROUP, role);
        });
    }

    protected UserDetails retrieveSolicitorUserDetails() {
        final String username = SOLICITOR_USER_NAME + EMAIL_DOMAIN;
        return getCreatedUserDetails(username, SOLICITOR_PASSWORD);
    }

    private UserDetails getUserDetails(String username, String userGroup,boolean keepUser, String... role) {
        synchronized (this) {
            idamTestSupportUtil.createUser(username, PASSWORD, userGroup, role);

            final String authToken = idamTestSupportUtil.generateUserTokenWithNoRoles(username, PASSWORD);

            final String userId = idamTestSupportUtil.getUserId(authToken);
            UserDetails userDetails = UserDetails.builder()
                .username(username)
                .emailAddress(username)
                .password(PASSWORD)
                .authToken(authToken)
                .id(userId)
                .build();

            if (!keepUser) {
                createdUsers.add(userDetails);
            }

            return userDetails;
        }
    }

    private UserDetails getUserDetails(String username, String userGroup, String... role) {
        return getUserDetails(username, userGroup, false, role);
    }

    private UserDetails getCreatedUserDetails(String username, String password) {
        synchronized (this) {
            final String authToken = idamTestSupportUtil.generateUserTokenWithNoRoles(username, password);

            final String userId = idamTestSupportUtil.getUserId(authToken);

            return UserDetails.builder()
                .username(username)
                .emailAddress(username)
                .password(password)
                .authToken(authToken)
                .id(userId)
                .build();
        }
    }

    private void deleteUser(UserDetails userDetails) {
        idamTestSupportUtil.deleteUser(userDetails);
    }

    private UserDetails wrapInRetry(Supplier<UserDetails> supplier) {
        //tactical solution as sometimes the newly created user is somehow corrupted and won't generate a code..
        int count = 0;
        int maxTries = 5;
        while (true) {
            try {
                return supplier.get();
            } catch (Exception e) {
                if (++count == maxTries) {
                    log.error("Exhausted the number of maximum retry attempts..", e);
                    throw e;
                }
                try {
                    //some backoff time
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    log.error("Error during sleep", ex);
                }
                log.trace("Encountered an error creating a user/token - retrying", e);
            }
        }
    }

    protected String getS2sAuth() {
        Response response = SerenityRest.given()
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .relaxedHTTPSValidation()
            .body(String.format("{\"microservice\": \"divorce_frontend\"}"))
            .post(idamS2sAuthUrl + "/testing-support/lease");

        String token = response.getBody().asString();

        return "Bearer " + token;
    }

}
