package uk.gov.hmcts.reform.divorce.orchestration.util;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.CcdFields.JUDGE_COSTS_CLAIM_GRANTED;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.NO_VALUE;
import static uk.gov.hmcts.reform.divorce.orchestration.domain.model.OrchestrationConstants.YES_VALUE;
import static uk.gov.hmcts.reform.divorce.orchestration.util.JudgeDecisionHelper.isJudgeCostClaimAdjourned;
import static uk.gov.hmcts.reform.divorce.orchestration.util.JudgeDecisionHelper.isJudgeCostClaimEmpty;
import static uk.gov.hmcts.reform.divorce.orchestration.util.JudgeDecisionHelper.isJudgeCostClaimGranted;
import static uk.gov.hmcts.reform.divorce.orchestration.util.JudgeDecisionHelper.isJudgeCostClaimRejected;

public class JudgeDecisionHelperTest {

    @Test
    public void isJudgeCostClaimGrantedReturnsTrue() {
        assertThat(isJudgeCostClaimGranted(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, YES_VALUE)), is(true));
    }

    @Test
    public void isJudgeCostClaimGrantedReturnsFalse() {
        assertThat(isJudgeCostClaimGranted(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, NO_VALUE)), is(false));
    }

    @Test
    public void isJudgeCostClaimRejectedReturnsTrue() {
        assertThat(isJudgeCostClaimRejected(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, NO_VALUE)), is(true));
    }

    @Test
    public void isJudgeCostClaimRejectedReturnsFalse() {
        assertThat(isJudgeCostClaimRejected(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, YES_VALUE)), is(false));
    }


    @Test
    public void isJudgeCostClaimAdjournedReturnsTrue() {
        assertThat(isJudgeCostClaimAdjourned(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, JudgeDecisionHelper.JudgeDecisions.ADJOURN_VALUE)), is(true));
    }

    @Test
    public void isJudgeCostClaimEmptyReturnsTrue() {
        assertThat(isJudgeCostClaimEmpty(createCaseData(JUDGE_COSTS_CLAIM_GRANTED, null)), is(true));
    }

    private static Map<String, Object> createCaseData(String field, Object value) {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put(field, value);

        return caseData;
    }
}