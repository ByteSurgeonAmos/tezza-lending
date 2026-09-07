package com.tezza.lending.shared;

import com.tezza.lending.shared.util.LoanNumberGenerator;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;

class LoanNumberGeneratorTest {

    @Test
    void generatesLoanNumberWithCorrectPrefix() {
        String number = LoanNumberGenerator.generate();
        assertThat(number).startsWith("TZ-" + Year.now().getValue() + "-");
    }

    @Test
    void generatedNumberHasEightDigitSuffix() {
        String number = LoanNumberGenerator.generate();
        String suffix = number.substring(number.lastIndexOf('-') + 1);
        assertThat(suffix).hasSize(8).matches("\\d{8}");
    }

    @Test
    void twoGeneratedNumbersAreDifferent() {
        String n1 = LoanNumberGenerator.generate();
        String n2 = LoanNumberGenerator.generate();
        assertThat(n1).isNotEqualTo(n2);
    }
}
