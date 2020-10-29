package uk.gov.hmcts.reform.divorce.orchestration.testutil;

import com.fasterxml.jackson.core.type.TypeReference;
import org.joda.time.LocalDate;
import uk.gov.hmcts.reform.divorce.model.ccd.CoreCaseData;
import uk.gov.hmcts.reform.divorce.model.usersession.DivorceSession;

import java.io.IOException;
import java.util.Map;

import static uk.gov.hmcts.reform.divorce.orchestration.testutil.ObjectMapperTestUtil.getJsonFromResourceFile;

public class DataTransformationTestHelper {

    public static DivorceSession getTestDivorceSessionData() throws IOException {
        return getJsonFromResourceFile("/jsonExamples/payloads/transformations/divorcetoccd/divorce/case-data.json", DivorceSession.class);
    }

    public static CoreCaseData getExpectedTranslatedCoreCaseData() throws IOException {
        CoreCaseData coreCaseData = getJsonFromResourceFile("/jsonExamples/payloads/transformations/divorcetoccd/ccd/case-data.json", CoreCaseData.class);
        coreCaseData.setCreatedDate(LocalDate.now().toString());
        return coreCaseData;
    }

    public static CoreCaseData getTestCoreCaseData() throws IOException {
        return getJsonFromResourceFile("/jsonExamples/payloads/transformations/ccdtodivorce/ccd/case-data.json", CoreCaseData.class);
    }

    public static DivorceSession getExpectedTranslatedDivorceSessionData() throws IOException {
        //        coreCaseData.setCreatedDate(LocalDate.now().toString());//TODO - dwt
        return getJsonFromResourceFile("/jsonExamples/payloads/transformations/ccdtodivorce/divorce/case-data.json", DivorceSession.class);
    }

    public static Map<String, Object> getExpectedTranslatedDivorceSessionJsonAsMap() throws IOException {
        //        coreCaseData.setCreatedDate(LocalDate.now().toString());//TODO - dwt
        DivorceSession expectedTranslatedDivorceSessionData = getExpectedTranslatedDivorceSessionData();
//        String json = ObjectMapperTestUtil.convertObjectToJsonString(expectedTranslatedDivorceSessionData);
        return ObjectMapperTestUtil.getObjectMapperInstance().convertValue(expectedTranslatedDivorceSessionData, new TypeReference<Map<String, Object>>() {
        });
    }//TODO - candidate for the above method

}