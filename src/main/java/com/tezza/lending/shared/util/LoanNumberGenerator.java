package com.tezza.lending.shared.util;

import java.time.Year;
import java.util.Random;

public final class LoanNumberGenerator {

    private static final Random RANDOM = new Random();

    private LoanNumberGenerator() {}

    public static String generate() {
        int year = Year.now().getValue();
        long suffix = (long) (RANDOM.nextDouble() * 100_000_000L);
        return String.format("TZ-%d-%08d", year, suffix);
    }
}
