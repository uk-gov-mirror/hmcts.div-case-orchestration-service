package uk.gov.hmcts.reform.divorce.orchestration.tasks;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.divorce.service.DataMapTransformer;

import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_INCOMING_PAYLOAD;
import static uk.gov.hmcts.reform.divorce.orchestration.TestConstants.TEST_PAYLOAD_TO_RETURN;

@RunWith(MockitoJUnitRunner.class)
public class CaseDataToDivorceFormatterUTest {

    @Mock
    private DataMapTransformer dataMapTransformer;

    @InjectMocks
    private CaseDataToDivorceFormatter classUnderTest;

    @Test
    public void whenFormatData_thenReturnExpectedData() {
        final Map<String, Object> expectedResults = TEST_PAYLOAD_TO_RETURN;
        when(dataMapTransformer.transformCoreCaseDataToDivorceCaseData(TEST_INCOMING_PAYLOAD)).thenReturn(expectedResults);

        Map<String, Object> returnedCaseData = classUnderTest.execute(null, TEST_INCOMING_PAYLOAD);

        assertThat(returnedCaseData, is(expectedResults));
        verify(dataMapTransformer).transformCoreCaseDataToDivorceCaseData(TEST_INCOMING_PAYLOAD);
    }

}