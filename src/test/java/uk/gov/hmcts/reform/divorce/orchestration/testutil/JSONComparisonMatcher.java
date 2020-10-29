package uk.gov.hmcts.reform.divorce.orchestration.testutil;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.skyscreamer.jsonassert.JSONAssert;

public class JSONComparisonMatcher extends TypeSafeMatcher<String> {

    private final String expectedJson;

    public JSONComparisonMatcher(String expectedJson) {
        this.expectedJson = expectedJson;
    }

    @Override
    protected boolean matchesSafely(String actualJson) {
        boolean match;
        try {
            JSONAssert.assertEquals(expectedJson, actualJson, false);//TODO - try strict when this works?
            match = true;
        } catch (AssertionError assertionError) {
            //TODO
            match = false;
        }

        return match;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("testing...");//TODO
    }

}