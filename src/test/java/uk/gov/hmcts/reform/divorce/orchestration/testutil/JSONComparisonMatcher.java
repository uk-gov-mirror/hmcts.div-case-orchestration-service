package uk.gov.hmcts.reform.divorce.orchestration.testutil;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.StringDescription;
import org.skyscreamer.jsonassert.JSONAssert;

public class JSONComparisonMatcher extends BaseMatcher {//TODO - once this works, try it with something other than Object

    private final String expectedJson;
    private String errorMessage;

    public JSONComparisonMatcher(String expectedJson) {
        this.expectedJson = expectedJson;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expectedJson);
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        description.appendText(errorMessage);
    }

    @Override
    public boolean matches(Object actual) {//TODO - maybe type is map
        boolean match;

        try {
            JSONAssert.assertEquals(expectedJson, ObjectMapperTestUtil.convertObjectToJsonString(actual), false);//TODO - try strict when this works?
            match = true;
        } catch (AssertionError assertionError) {
            //TODO
            errorMessage = assertionError.getMessage();
            throw assertionError;
//            match = false;
        }

        return match;
    }

}