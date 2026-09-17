package com.coldchain.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorCategoryStatusTest {

    @Test
    void everyCategoryMapsToExactlyOneStatus() {
        for (ErrorCategory category : ErrorCategory.values()) {
            assertThat(ProblemDetails.statusOf(category))
                    .describedAs("category %s has no status", category)
                    .isNotNull();
        }
    }

    @Test
    void aBusinessRuleFailureIsFourTwentyTwoAndNeverFiveHundred() {
        assertThat(ProblemDetails.statusOf(ErrorCategory.BUSINESS_RULE))
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void aConcurrencyConflictIsFourZeroNine() {
        assertThat(ProblemDetails.statusOf(ErrorCategory.CONFLICT)).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void onlyTheInternalCategoryIsAServerError() {
        for (ErrorCategory category : ErrorCategory.values()) {
            boolean serverError = ProblemDetails.statusOf(category).is5xxServerError();
            assertThat(serverError)
                    .describedAs("category %s", category)
                    .isEqualTo(category == ErrorCategory.INTERNAL || category == ErrorCategory.INTEGRATION);
        }
    }
}
