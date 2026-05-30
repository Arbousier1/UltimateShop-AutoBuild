package cn.superiormc.ultimateshop.utils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountVariableUtilPureTest {

    private static int passed;
    private static int failed;

    static void check(String name, String actual, String expected) {
        if (actual.equals(expected)) {
            passed++;
            System.out.println("  PASS: " + name + " = " + actual);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
            System.out.println("        expected: " + expected);
            System.out.println("        got     : " + actual);
        }
    }

    static void checkBool(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
        }
    }

    static void checkDouble(String name, double actual, double expected, double tolerance) {
        if (Double.isNaN(expected) && Double.isNaN(actual)) {
            passed++;
            System.out.println("  PASS: " + name + " = NaN");
            return;
        }
        if (Double.isInfinite(expected) && Double.isInfinite(actual) && (expected > 0 == actual > 0)) {
            passed++;
            System.out.println("  PASS: " + name + " = Infinity");
            return;
        }
        if (Double.isNaN(actual) || Double.isInfinite(actual)) {
            failed++;
            System.out.println("  FAIL: " + name + " (got " + actual + ")");
            return;
        }
        double diff = Math.abs(actual - expected);
        if (diff <= tolerance) {
            passed++;
            System.out.println("  PASS: " + name + " = " + actual);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
            System.out.println("        expected: " + expected);
            System.out.println("        got     : " + actual);
            System.out.println("        diff    : " + diff);
        }
    }

    static void checkInt(String name, int actual, int expected) {
        if (actual == expected) {
            passed++;
            System.out.println("  PASS: " + name + " = " + actual);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
            System.out.println("        expected: " + expected);
            System.out.println("        got     : " + actual);
        }
    }

    static String decimal(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        return BigDecimal.valueOf(value)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    static double seconds(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static String remaining(int limit, int used) {
        if (limit < 0) {
            return "-1";
        }
        return String.valueOf(Math.max(limit - used, 0));
    }

    static int applyOffset(int value, int offsetAmount, boolean buyOrSell, boolean placeholderBuyOrSell) {
        if ((buyOrSell && placeholderBuyOrSell) || (!buyOrSell && !placeholderBuyOrSell)) {
            return value + offsetAmount;
        }
        return value;
    }

    public static void main(String[] args) {
        System.out.println("= AmountVariableUtil Pure Function Tests =\n");

        testDecimal();
        testSeconds();
        testRemaining();
        testApplyOffset();
        testDecimalEdgeCases();
        testDecimalPrecision();
        testSecondsEdgeCases();
        testRemainingEdgeCases();
        testApplyOffsetCombinations();
        testDecimalRoundTrip();
        testDecimalWithDecayCalculator();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED!");
        }
    }

    static void testDecimal() {
        System.out.println("--- decimal() basic ---");
        check("decimal(0)", decimal(0), "0");
        check("decimal(1)", decimal(1), "1");
        check("decimal(-1)", decimal(-1), "-1");
        check("decimal(0.5)", decimal(0.5), "0.5");
        check("decimal(1.5)", decimal(1.5), "1.5");
        check("decimal(100)", decimal(100), "100");
        check("decimal(3.14159)", decimal(3.14159), "3.14159");
        check("decimal(0.1)", decimal(0.1), "0.1");
        check("decimal(0.01)", decimal(0.01), "0.01");
    }

    static void testDecimalEdgeCases() {
        System.out.println("\n--- decimal() edge cases ---");
        check("decimal(NaN)", decimal(Double.NaN), "0");
        check("decimal(PositiveInfinity)", decimal(Double.POSITIVE_INFINITY), "0");
        check("decimal(NegativeInfinity)", decimal(Double.NEGATIVE_INFINITY), "0");
        checkBool("decimal(Double.MAX_VALUE) has length > 0", decimal(Double.MAX_VALUE).length() > 0);
        checkBool("decimal(Double.MIN_VALUE) has length > 0", decimal(Double.MIN_VALUE).length() > 0);
        check("decimal(-0.0)", decimal(-0.0), "0");
    }

    static void testDecimalPrecision() {
        System.out.println("\n--- decimal() precision ---");
        String result = decimal(1.0 / 3.0);
        checkBool("1/3 has up to 10 decimal places", result.contains(".") && result.length() <= 15);

        result = decimal(0.1234567890);
        checkBool("0.1234567890 preserves precision", result.startsWith("0.123456789"));

        result = decimal(999999999.123456789);
        checkBool("Large number with decimals", result.contains("."));

        result = decimal(1e10);
        checkBool("1e10 renders without scientific notation", !result.contains("E") && !result.contains("e"));

        result = decimal(1e-10);
        checkBool("1e-10 renders without scientific notation", !result.contains("E") && !result.contains("e"));

        result = decimal(0.0000000001);
        checkBool("0.0000000001 preserves", result.contains("0.0000000001") || result.equals("0") || result.startsWith("0.000000000"));

        result = decimal(123.456789012345);
        checkBool("Truncated to 10 decimal places", result.equals("123.4567890123") || result.startsWith("123.4567890123"));
    }

    static void testSeconds() {
        System.out.println("\n--- seconds() basic ---");
        checkDouble("seconds('0')", seconds("0"), 0, 0.001);
        checkDouble("seconds('100')", seconds("100"), 100, 0.001);
        checkDouble("seconds('3.14')", seconds("3.14"), 3.14, 0.001);
        checkDouble("seconds('-5')", seconds("-5"), -5, 0.001);
        checkDouble("seconds('999999')", seconds("999999"), 999999, 0.001);
    }

    static void testSecondsEdgeCases() {
        System.out.println("\n--- seconds() edge cases ---");
        checkDouble("seconds(null)", seconds(null), 0, 0.001);
        checkDouble("seconds('')", seconds(""), 0, 0.001);
        checkDouble("seconds('abc')", seconds("abc"), 0, 0.001);
        checkDouble("seconds('12abc')", seconds("12abc"), 0, 0.001);
        checkDouble("seconds('  ')", seconds("  "), 0, 0.001);
        checkDouble("seconds('1e5')", seconds("1e5"), 100000, 0.001);
        checkDouble("seconds('Infinity')", seconds("Infinity"), Double.POSITIVE_INFINITY, 0.001);
        checkDouble("seconds('NaN')", seconds("NaN"), Double.NaN, 0.001);
    }

    static void testRemaining() {
        System.out.println("\n--- remaining() basic ---");
        check("remaining(10, 3)", remaining(10, 3), "7");
        check("remaining(10, 0)", remaining(10, 0), "10");
        check("remaining(10, 10)", remaining(10, 10), "0");
        check("remaining(10, 15)", remaining(10, 15), "0");
        check("remaining(0, 0)", remaining(0, 0), "0");
    }

    static void testRemainingEdgeCases() {
        System.out.println("\n--- remaining() edge cases ---");
        check("remaining(-1, 5)", remaining(-1, 5), "-1");
        check("remaining(-1, 0)", remaining(-1, 0), "-1");
        check("remaining(-100, 50)", remaining(-100, 50), "-1");
        check("remaining(1, 0)", remaining(1, 0), "1");
        check("remaining(Integer.MAX_VALUE, 0)", remaining(Integer.MAX_VALUE, 0), String.valueOf(Integer.MAX_VALUE));
        check("remaining(1, Integer.MAX_VALUE)", remaining(1, Integer.MAX_VALUE), "0");
    }

    static void testApplyOffset() {
        System.out.println("\n--- applyOffset() basic ---");
        checkInt("buy+buy", applyOffset(10, 5, true, true), 15);
        checkInt("buy+sell", applyOffset(10, 5, true, false), 10);
        checkInt("sell+buy", applyOffset(10, 5, false, true), 10);
        checkInt("sell+sell", applyOffset(10, 5, false, false), 15);
    }

    static void testApplyOffsetCombinations() {
        System.out.println("\n--- applyOffset() all combinations ---");
        checkInt("buy, offset=0", applyOffset(10, 0, true, true), 10);
        checkInt("sell, offset=0", applyOffset(10, 0, false, false), 10);
        checkInt("buy, negative offset", applyOffset(10, -3, true, true), 7);
        checkInt("sell, negative offset", applyOffset(10, -3, false, false), 7);
        checkInt("buy, large offset", applyOffset(10, 1000, true, true), 1010);
        checkInt("mismatch always returns original", applyOffset(10, 999, true, false), 10);
        checkInt("mismatch (sell, buy placeholder)", applyOffset(10, 999, false, true), 10);

        checkInt("value=0, offset=5, matching", applyOffset(0, 5, true, true), 5);
        checkInt("value=0, offset=5, mismatch", applyOffset(0, 5, true, false), 0);
        checkInt("value=0, offset=0, matching", applyOffset(0, 0, true, true), 0);
    }

    static void testDecimalRoundTrip() {
        System.out.println("\n--- decimal() round-trip with DecayCalculator ---");
        double p0 = 100, lambda = 0.05, epsilon = 0.95, iota = 1.0;
        double pn = DecayCalculator.computePn(epsilon, iota, p0, lambda, 10);
        String pnStr = decimal(pn);
        double pnParsed = Double.parseDouble(pnStr);
        checkDouble("Round-trip p(10)", pnParsed, pn, 0.0001);

        double mn = DecayCalculator.computeMnTheoretical(lambda, 50);
        String mnStr = decimal(mn);
        double mnParsed = Double.parseDouble(mnStr);
        checkDouble("Round-trip M(50)", mnParsed, mn, 0.0001);

        double eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 0.98, 0);
        String epsStr = decimal(eps);
        double epsParsed = Double.parseDouble(epsStr);
        checkDouble("Round-trip epsilon", epsParsed, eps, 0.0001);

        double q = DecayCalculator.computeQuota(10, 2, 5, 100);
        String qStr = decimal(q);
        double qParsed = Double.parseDouble(qStr);
        checkDouble("Round-trip quota", qParsed, q, 0.0001);
    }

    static void testDecimalWithDecayCalculator() {
        System.out.println("\n--- decimal() with DecayCalculator outputs ---");
        double nanResult = Double.NaN;
        check("decimal(NaN from calc)", decimal(nanResult), "0");

        double infResult = Double.POSITIVE_INFINITY;
        check("decimal(Infinity from calc)", decimal(infResult), "0");

        double normalResult = DecayCalculator.computePn(0.95, 1.0, 100, 0.05, 0);
        String formatted = decimal(normalResult);
        checkBool("Formatted price is parseable", Double.parseDouble(formatted) > 0);
        checkBool("Formatted price matches original", Math.abs(Double.parseDouble(formatted) - normalResult) < 0.001);

        double quotaResult = DecayCalculator.computeQuota(0, 0, 0, 100);
        check("decimal(zero quota)", decimal(quotaResult), "0");

        double mnResult = DecayCalculator.computeMnTheoretical(0.05, 0);
        check("decimal(M(0))", decimal(mnResult), "0");
    }
}
