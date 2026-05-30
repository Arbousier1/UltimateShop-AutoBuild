package cn.superiormc.ultimateshop.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DecayCalculatorTest {

    private static int passed;
    private static int failed;
    private static final double SECONDS_PER_DAY = 86400.0;

    static void check(String name, double actual, double expected, double tolerance) {
        if (Double.isNaN(actual) || Double.isInfinite(actual)) {
            failed++;
            System.out.println("  FAIL: " + name + " (got " + actual + ", expected " + expected + ")");
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
            System.out.println("        diff    : " + diff + " (tolerance: " + tolerance + ")");
        }
    }

    static void checkInt(String name, double actual, int expected) {
        check(name, actual, expected, 0.5);
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

    static void checkThrows(String name, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL: " + name + " (no exception thrown)");
        } catch (Exception e) {
            passed++;
            System.out.println("  PASS: " + name + " (threw " + e.getClass().getSimpleName() + ")");
        }
    }

    static void checkNoThrow(String name, Runnable action) {
        try {
            action.run();
            passed++;
            System.out.println("  PASS: " + name + " (no exception)");
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    public static void main(String[] args) {
        System.out.println("= DecayCalculator + Article Model Comprehensive Tests =\n");

        testPeriodRecordConstruction();
        testSinglePeriodDecaySell();
        testSinglePeriodDecayBuy();
        testMultiPeriodDecay();
        testDecayEdgeCases();
        testDecayNegativeElapsed();
        testDecayVeryLargeDelta();
        testDecayVerySmallDelta();
        testHolidayImpact();
        testHolidayImpactSymmetry();
        testHolidayImpactNumericalStability();
        testEpsilonCalculation();
        testEpsilonBounds();
        testEpsilonAllHolidays();
        testQuotaCalculation();
        testQuotaEdgeCases();
        testDecayHistoryDays();
        testDecayHistoryDaysEdgeCases();
        testMnTheoretical();
        testMnTheoreticalConvergence();
        testMnTheoreticalNumericalStability();
        testPnCalculation();
        testPnEdgeCases();
        testPnMonotonicity();
        testTrimHistory();
        testTrimHistoryEdgeCases();
        testArticleFullFormula();
        testArticleFormulaConsistency();
        testNumericalOverflowUnderflow();
        testConcurrentAccess();
        testPerformance();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED!");
        }
    }

    static void testPeriodRecordConstruction() {
        System.out.println("--- PeriodRecord construction ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        DecayCalculator.PeriodRecord r = new DecayCalculator.PeriodRecord(5, 10, now);
        checkInt("buyTimes = 5", r.buyTimes, 5);
        checkInt("sellTimes = 10", r.sellTimes, 10);
        checkBool("resetTime matches", r.resetTime.equals(now));

        DecayCalculator.PeriodRecord nullTime = new DecayCalculator.PeriodRecord(0, 0, null);
        checkInt("null time buyTimes = 0", nullTime.buyTimes, 0);
        checkInt("null time sellTimes = 0", nullTime.sellTimes, 0);
        checkBool("null time resetTime is null", nullTime.resetTime == null);

        DecayCalculator.PeriodRecord large = new DecayCalculator.PeriodRecord(Integer.MAX_VALUE, Integer.MAX_VALUE, now);
        checkInt("max buyTimes", large.buyTimes, Integer.MAX_VALUE);
        checkInt("max sellTimes", large.sellTimes, Integer.MAX_VALUE);
    }

    static void testSinglePeriodDecaySell() {
        System.out.println("\n--- Single period decay (sell) ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(1)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("1 day ago, n=10, tau=7 (FLOOR makes n-1)", result, 9.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("7 days ago (at tau), n=10", result, 5.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(14)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("14 days ago, n=10 (well past tau)", result, 0.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("7 days ago (at tau), n=100", result, 50.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(3)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("3 days ago, n=10 (FLOOR makes n-1)", result, 9.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 1000, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("at tau, n=1000 => ~500", result, 500.0, 1.0);
    }

    static void testSinglePeriodDecayBuy() {
        System.out.println("\n--- Single period decay (buy) ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(10, 0, now.minusDays(1)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, false, delta, tauDays, now);
        check("buy: 1 day ago, n=10 (FLOOR makes n-1)", result, 9.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(10, 0, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, false, delta, tauDays, now);
        check("buy: 7 days ago (at tau), n=10", result, 5.0, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(10, 5, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("mixed record, isSell=true => sellTimes=5", result, 2.5, 0.5);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(10, 5, now.minusDays(7)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, false, delta, tauDays, now);
        check("mixed record, isSell=false => buyTimes=10", result, 5.0, 0.5);
    }

    static void testMultiPeriodDecay() {
        System.out.println("\n--- Multi-period decay ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(1)));
        history.add(new DecayCalculator.PeriodRecord(0, 5, now.minusDays(7)));
        history.add(new DecayCalculator.PeriodRecord(0, 20, now.minusDays(14)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        check("3 periods: 10@1d + 5@7d + 20@14d", result, 10 + 2 + 0, 1.0);

        history.clear();
        for (int i = 1; i <= 10; i++) {
            history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(i)));
        }
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        checkBool("10 periods of 10 each, result > 0", result > 0);
        checkBool("10 periods of 10 each, result < 100", result < 100);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(1)));
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(2)));
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.minusDays(3)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        double singleResult = 0;
        for (DecayCalculator.PeriodRecord r : history) {
            singleResult += Math.floor(10 / (Math.exp(delta * (ChronoUnit.SECONDS.between(r.resetTime, now) / SECONDS_PER_DAY - tauDays)) + 1));
        }
        check("3 close periods match manual sum", result, singleResult, 0.01);
    }

    static void testDecayEdgeCases() {
        System.out.println("\n--- Edge cases ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);

        List<DecayCalculator.PeriodRecord> emptyHistory = new ArrayList<>();
        double result = DecayCalculator.computeDecayedFromHistory(emptyHistory, 0, null, true, 1.0, 7.0, now);
        checkInt("Empty history, no current", result, 0);

        result = DecayCalculator.computeDecayedFromHistory(emptyHistory, 5, now.minusDays(1), true, 1.0, 7.0, now);
        check("Empty history, current=5 (FLOOR makes 4)", result, 4.0, 0.5);

        List<DecayCalculator.PeriodRecord> zeroHistory = new ArrayList<>();
        zeroHistory.add(new DecayCalculator.PeriodRecord(0, 0, now.minusDays(1)));
        result = DecayCalculator.computeDecayedFromHistory(zeroHistory, 0, null, true, 1.0, 7.0, now);
        checkInt("Zero sell times in history", result, 0);

        List<DecayCalculator.PeriodRecord> nullTimeHistory = new ArrayList<>();
        nullTimeHistory.add(new DecayCalculator.PeriodRecord(0, 10, null));
        result = DecayCalculator.computeDecayedFromHistory(nullTimeHistory, 0, null, true, 1.0, 7.0, now);
        check("Null resetTime, n=10 (FLOOR makes n-1)", result, 9.0, 0.5);

        result = DecayCalculator.computeDecayedFromHistory(emptyHistory, 10, null, true, 1.0, 7.0, now);
        checkInt("No history, current=10, null resetTime", result, 10);

        List<DecayCalculator.PeriodRecord> multipleZero = new ArrayList<>();
        multipleZero.add(new DecayCalculator.PeriodRecord(0, 0, now.minusDays(1)));
        multipleZero.add(new DecayCalculator.PeriodRecord(0, 0, now.minusDays(2)));
        multipleZero.add(new DecayCalculator.PeriodRecord(0, 0, now.minusDays(3)));
        result = DecayCalculator.computeDecayedFromHistory(multipleZero, 0, null, true, 1.0, 7.0, now);
        checkInt("Multiple zero records", result, 0);

        result = DecayCalculator.computeDecayedFromHistory(emptyHistory, 0, now.minusDays(1), true, 1.0, 7.0, now);
        checkInt("currentTimes=0 with resetTime", result, 0);
    }

    static void testDecayNegativeElapsed() {
        System.out.println("\n--- Decay with negative elapsed (resetTime after now) ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 10, now.plusDays(1)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        checkBool("Future resetTime, result >= 0", result >= 0);
        checkBool("Future resetTime, result finite", Double.isFinite(result));

        result = DecayCalculator.computeDecayedFromHistory(new ArrayList<>(), 10, now.plusDays(1), true, delta, tauDays, now);
        checkBool("Future currentResetTime, result >= 0", result >= 0);
        checkBool("Future currentResetTime, result finite", Double.isFinite(result));
    }

    static void testDecayVeryLargeDelta() {
        System.out.println("\n--- Decay with very large delta ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(8)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, 100.0, 7.0, now);
        check("Very large delta, 1 day past tau", result, 0.0, 1.0);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(6)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, 100.0, 7.0, now);
        check("Very large delta, 1 day before tau", result, 100.0, 1.0);
    }

    static void testDecayVerySmallDelta() {
        System.out.println("\n--- Decay with very small delta ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(7)));
        double result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, 0.001, 7.0, now);
        check("Very small delta at tau, ~50", result, 50.0, 2.0);

        history.clear();
        history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(14)));
        result = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, 0.001, 7.0, now);
        checkBool("Very small delta, 14 days ago, still significant decay", result > 10);
        checkBool("Very small delta, 14 days ago, not fully decayed", result < 100);
    }

    static void testHolidayImpact() {
        System.out.println("\n--- Holiday impact ---");
        double alpha = 0.15;
        double mu = 46;
        double sigma = 65536;

        double impact = DecayCalculator.holidayImpact(alpha, mu, sigma, 46);
        check("At mu (day 46), impact = alpha", impact, alpha, 0.001);

        impact = DecayCalculator.holidayImpact(alpha, mu, sigma, 1);
        checkBool("Far from mu (day 1) with large sigma, impact < alpha", impact < alpha);

        impact = DecayCalculator.holidayImpact(0, mu, sigma, 46);
        checkInt("Alpha=0, impact=0", impact, 0);

        impact = DecayCalculator.holidayImpact(alpha, mu, 0, 46);
        checkInt("Sigma=0, impact=0", impact, 0);

        impact = DecayCalculator.holidayImpact(alpha, mu, -1, 46);
        checkInt("Sigma<0, impact=0", impact, 0);

        double alphaNational = 0.075;
        double muNational = 274;
        double sigmaNational = 33.1776;
        impact = DecayCalculator.holidayImpact(alphaNational, muNational, sigmaNational, 274);
        check("National day at mu, impact = alpha", impact, alphaNational, 0.001);

        impact = DecayCalculator.holidayImpact(alphaNational, muNational, sigmaNational, 260);
        checkBool("National day 14 days before, impact < alpha", impact < alphaNational);

        impact = DecayCalculator.holidayImpact(alphaNational, muNational, sigmaNational, 288);
        checkBool("National day 14 days after, impact < alpha", impact < alphaNational);

        impact = DecayCalculator.holidayImpact(alpha, mu, sigma, 365);
        checkBool("Day 365 far from mu=46, impact ~ 0", impact < 0.001);
    }

    static void testHolidayImpactSymmetry() {
        System.out.println("\n--- Holiday impact symmetry (offset^6) ---");
        double alpha = 0.15;
        double mu = 46;
        double sigma = 65536;

        double before = DecayCalculator.holidayImpact(alpha, mu, sigma, 36);
        double after = DecayCalculator.holidayImpact(alpha, mu, sigma, 56);
        check("Symmetric offset: 10 days before == 10 days after", before, after, 0.0001);

        before = DecayCalculator.holidayImpact(alpha, mu, sigma, 30);
        after = DecayCalculator.holidayImpact(alpha, mu, sigma, 62);
        check("Symmetric offset: 16 days before == 16 days after", before, after, 0.0001);

        double atMu = DecayCalculator.holidayImpact(alpha, mu, sigma, (int) mu);
        checkBool("At mu is maximum", atMu >= before && atMu >= after);
    }

    static void testHolidayImpactNumericalStability() {
        System.out.println("\n--- Holiday impact numerical stability ---");
        double alpha = 0.15;
        double mu = 46;
        double sigma = 65536;

        double impact = DecayCalculator.holidayImpact(alpha, mu, sigma, 0);
        checkBool("Day 0, impact finite", Double.isFinite(impact));
        checkBool("Day 0, impact >= 0", impact >= 0);

        impact = DecayCalculator.holidayImpact(alpha, mu, sigma, 366);
        checkBool("Day 366, impact finite", Double.isFinite(impact));
        checkBool("Day 366, impact >= 0", impact >= 0);

        impact = DecayCalculator.holidayImpact(alpha, mu, sigma, Integer.MAX_VALUE);
        checkBool("Very large day, impact finite", Double.isFinite(impact));

        impact = DecayCalculator.holidayImpact(alpha, mu, sigma, -1);
        checkBool("Negative day, impact finite", Double.isFinite(impact));
        checkBool("Negative day, impact >= 0", impact >= 0);

        impact = DecayCalculator.holidayImpact(Double.MAX_VALUE, (int) mu, sigma, (int) mu);
        checkBool("Very large alpha at mu, impact finite", Double.isFinite(impact));
    }

    static void testEpsilonCalculation() {
        System.out.println("\n--- Epsilon calculation ---");
        double eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0);
        check("No holidays, beta=1, noise=0 => epsilon=1", eps, 1.0, 0.001);

        eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.98, 0);
        check("No holidays, beta=0.98 => epsilon=0.98", eps, 0.98, 0.001);

        eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0.05);
        check("No holidays, noise=0.05 => epsilon=1.05", eps, 1.05, 0.001);

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 1.0, 0);
        check("Winter at mu, alpha=0.15 => epsilon=0.85", eps, 0.85, 0.001);

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0.15, 46, 65536, 0, 0, 0, 1.0, 0);
        check("Winter+Summer at same mu => epsilon=0.70", eps, 0.70, 0.001);

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0.15, 46, 65536, 0.075, 46, 65536, 1.0, 0);
        check("All 3 holidays at same mu => epsilon=0.625", eps, 0.625, 0.001);

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 0.98, 0.02);
        check("Winter at mu + beta=0.98 + noise=0.02", eps, 0.85 * 0.98 + 0.02, 0.001);
    }

    static void testEpsilonBounds() {
        System.out.println("\n--- Epsilon bounds ---");
        double eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, -0.5);
        checkBool("Negative noise can make epsilon < 1", eps < 1.0);
        checkBool("Epsilon is finite", Double.isFinite(eps));

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0.15, 46, 65536, 0.075, 46, 65536, 1.0, 0);
        checkBool("Max holiday impact, epsilon > 0", eps > 0);
        checkBool("Max holiday impact, epsilon < 1", eps < 1);

        eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        check("Beta=0, no noise => epsilon=0", eps, 0.0, 0.001);

        eps = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0.5);
        check("Large positive noise => epsilon=1.5", eps, 1.5, 0.001);
    }

    static void testEpsilonAllHolidays() {
        System.out.println("\n--- Epsilon with all holidays at different days ---");
        double eps = DecayCalculator.computeEpsilon(1, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 1.0, 0);
        checkBool("Day 1 (large sigma winter), epsilon < 1", eps < 1.0);
        checkBool("Day 1 (large sigma winter), epsilon > 0.8", eps > 0.8);

        eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 1.0, 0);
        checkBool("Day 46 (winter), epsilon < 1", eps < 1.0);
        checkBool("Day 46 (winter), epsilon > 0.8", eps > 0.8);

        eps = DecayCalculator.computeEpsilon(274, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 1.0, 0);
        checkBool("Day 274 (national), epsilon < 1", eps < 1.0);
        checkBool("Day 274 (national), epsilon > 0.85", eps > 0.85);

        eps = DecayCalculator.computeEpsilon(200, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 0.95, 0);
        checkBool("Day 200 with beta=0.95, epsilon < 0.95", eps < 0.95);
    }

    static void testQuotaCalculation() {
        System.out.println("\n--- Quota calculation ---");
        double q = DecayCalculator.computeQuota(0, 0, 0, 100);
        check("Q_B=0, gamma=0, T=100 => Q=0", q, 0.0, 0.001);

        q = DecayCalculator.computeQuota(10, 0, 0, 100);
        check("Q_B=10, gamma=0, T=100 => Q=1000", q, 1000.0, 0.001);

        q = DecayCalculator.computeQuota(10, 2, 5, 100);
        check("Q_B=10, gamma=2, P=5, T=100 => Q=2000", q, 2000.0, 0.001);

        q = DecayCalculator.computeQuota(0, 1, 10, 50);
        check("Q_B=0, gamma=1, P=10, T=50 => Q=500", q, 500.0, 0.001);

        q = DecayCalculator.computeQuota(1, 1, 1, 1);
        check("All ones => Q=2", q, 2.0, 0.001);
    }

    static void testQuotaEdgeCases() {
        System.out.println("\n--- Quota edge cases ---");
        double q = DecayCalculator.computeQuota(0, 0, 0, 0);
        check("All zeros => Q=0", q, 0.0, 0.001);

        q = DecayCalculator.computeQuota(-1, 0, 0, 100);
        checkBool("Negative Q_B => Q < 0", q < 0);
        checkBool("Negative Q_B => finite", Double.isFinite(q));

        q = DecayCalculator.computeQuota(0, -1, 5, 100);
        checkBool("Negative gamma => Q < 0", q < 0);

        q = DecayCalculator.computeQuota(10, 0, 0, 0);
        check("T=0 => Q=0", q, 0.0, 0.001);

        q = DecayCalculator.computeQuota(0, 0, 5, 100);
        check("Q_B=0, gamma=0, P>0 => Q=0", q, 0.0, 0.001);
    }

    static void testDecayHistoryDays() {
        System.out.println("\n--- Decay history days ---");
        double days = DecayCalculator.computeDecayHistoryDays(7, 1, 100);
        check("tau=7, delta=1, nu=100", days, 7 + Math.log(99), 0.001);

        days = DecayCalculator.computeDecayHistoryDays(7, 1, 2);
        check("tau=7, delta=1, nu=2", days, 7.0, 0.001);

        days = DecayCalculator.computeDecayHistoryDays(7, 1, 1);
        checkInt("tau=7, delta=1, nu=1 => 0", days, 0);

        days = DecayCalculator.computeDecayHistoryDays(7, 0, 100);
        checkInt("tau=7, delta=0 => 0", days, 0);

        days = DecayCalculator.computeDecayHistoryDays(7, -1, 100);
        checkInt("tau=7, delta=-1 => 0", days, 0);
    }

    static void testDecayHistoryDaysEdgeCases() {
        System.out.println("\n--- Decay history days edge cases ---");
        double days = DecayCalculator.computeDecayHistoryDays(0, 1, 100);
        check("tau=0, delta=1, nu=100", days, Math.log(99), 0.001);

        days = DecayCalculator.computeDecayHistoryDays(7, 0.5, 100);
        check("tau=7, delta=0.5, nu=100", days, 7 + Math.log(99) / 0.5, 0.001);

        days = DecayCalculator.computeDecayHistoryDays(7, 1, 0);
        checkInt("nu=0 => 0", days, 0);

        days = DecayCalculator.computeDecayHistoryDays(7, 1, -1);
        checkInt("nu<0 => 0", days, 0);

        days = DecayCalculator.computeDecayHistoryDays(7, 1, Integer.MAX_VALUE);
        checkBool("Very large nu, result finite", Double.isFinite(days));
        checkBool("Very large nu, result > tau", days > 7);
    }

    static void testMnTheoretical() {
        System.out.println("\n--- M(n) theoretical ---");
        double mn = DecayCalculator.computeMnTheoretical(0.1, 0);
        check("M(0) = 0", mn, 0.0, 0.001);

        mn = DecayCalculator.computeMnTheoretical(0.1, 10);
        double expected = (1 - Math.exp(-1)) / 0.1;
        check("M(10) with lambda=0.1", mn, expected, 0.001);

        mn = DecayCalculator.computeMnTheoretical(0.05, 100);
        expected = (1 - Math.exp(-5)) / 0.05;
        check("M(100) with lambda=0.05", mn, expected, 0.001);

        mn = DecayCalculator.computeMnTheoretical(0, 10);
        checkInt("lambda=0 => M(n)=0", mn, 0);

        mn = DecayCalculator.computeMnTheoretical(-1, 10);
        checkInt("lambda<0 => M(n)=0", mn, 0);

        double prev = DecayCalculator.computeMnTheoretical(0.1, 5);
        double next = DecayCalculator.computeMnTheoretical(0.1, 10);
        checkBool("M(n) is monotonically increasing", next > prev);

        double m1 = DecayCalculator.computeMnTheoretical(0.1, 100);
        double m2 = DecayCalculator.computeMnTheoretical(0.1, 1000);
        checkBool("M(n) is concave (growth slows)", (m2 - m1) < (m1 - prev));
    }

    static void testMnTheoreticalConvergence() {
        System.out.println("\n--- M(n) convergence to 1/lambda ---");
        double lambda = 0.1;
        double limit = 1.0 / lambda;
        double mn100 = DecayCalculator.computeMnTheoretical(lambda, 100);
        checkBool("M(100) < 1/lambda", mn100 < limit);
        checkBool("M(100) approaches 1/lambda", mn100 > limit * 0.99);

        double mn1000 = DecayCalculator.computeMnTheoretical(lambda, 1000);
        checkBool("M(1000) closer to 1/lambda than M(100)", Math.abs(mn1000 - limit) < Math.abs(mn100 - limit));

        double lambdaSmall = 0.001;
        double limitSmall = 1.0 / lambdaSmall;
        double mnLarge = DecayCalculator.computeMnTheoretical(lambdaSmall, 10000);
        checkBool("Small lambda, large n, M(n) approaches 1/lambda", Math.abs(mnLarge - limitSmall) / limitSmall < 0.01);
    }

    static void testMnTheoreticalNumericalStability() {
        System.out.println("\n--- M(n) numerical stability ---");
        double mn = DecayCalculator.computeMnTheoretical(0.001, 1);
        checkBool("Small lambda, small n, finite", Double.isFinite(mn));
        checkBool("Small lambda, small n, > 0", mn > 0);

        mn = DecayCalculator.computeMnTheoretical(100, 1);
        checkBool("Large lambda, small n, finite", Double.isFinite(mn));
        checkBool("Large lambda, small n, > 0", mn > 0);

        mn = DecayCalculator.computeMnTheoretical(0.01, 100000);
        checkBool("Large n, finite", Double.isFinite(mn));
        checkBool("Large n, > 0", mn > 0);

        mn = DecayCalculator.computeMnTheoretical(Double.MIN_VALUE, 1);
        checkBool("Tiny lambda, finite", Double.isFinite(mn));

        mn = DecayCalculator.computeMnTheoretical(1e-10, 1e10);
        checkBool("Very small lambda + very large n, finite", Double.isFinite(mn));
    }

    static void testPnCalculation() {
        System.out.println("\n--- p(n) calculation ---");
        double pn = DecayCalculator.computePn(1, 1, 100, 0.1, 0);
        check("p(0) = epsilon*iota*p0", pn, 100.0, 0.001);

        pn = DecayCalculator.computePn(1, 1, 100, 0.1, 10);
        double expected = Math.round(100 * Math.exp(-1) * 100.0) / 100.0;
        check("p(10) with lambda=0.1 (rounded)", pn, expected, 0.001);

        pn = DecayCalculator.computePn(0.85, 1.25, 100, 0.05, 20);
        expected = Math.round(0.85 * 1.25 * 100 * Math.exp(-1) * 100.0) / 100.0;
        check("p(20) with epsilon=0.85, iota=1.25 (rounded)", pn, expected, 0.001);

        pn = DecayCalculator.computePn(1, 1, 100, 0.1, 0);
        double pnLater = DecayCalculator.computePn(1, 1, 100, 0.1, 10);
        checkBool("p(n) is monotonically decreasing", pnLater < pn);
    }

    static void testPnEdgeCases() {
        System.out.println("\n--- p(n) edge cases ---");
        double pn = DecayCalculator.computePn(0, 1, 100, 0.1, 0);
        check("epsilon=0 => p(n)=0.01 (floor)", pn, 0.01, 0.001);

        pn = DecayCalculator.computePn(1, 0, 100, 0.1, 0);
        check("iota=0 => p(n)=0.01 (floor)", pn, 0.01, 0.001);

        pn = DecayCalculator.computePn(1, 1, 0, 0.1, 0);
        check("p0=0 => p(n)=0.01 (floor)", pn, 0.01, 0.001);

        pn = DecayCalculator.computePn(1, 1, 100, 0, 10);
        check("lambda=0 => p(n)=epsilon*iota*p0", pn, 100.0, 0.001);

        pn = DecayCalculator.computePn(-1, 1, 100, 0.1, 0);
        check("Negative epsilon => p(n)=0.01 (floor)", pn, 0.01, 0.001);

        pn = DecayCalculator.computePn(1, 1, 100, 0.1, 1000000);
        check("Very large n => p(n)=0.01 (floor)", pn, 0.01, 0.001);
    }

    static void testPnMonotonicity() {
        System.out.println("\n--- p(n) strict monotonicity ---");
        double epsilon = 0.95;
        double iota = 1.1;
        double p0 = 100;
        double lambda = 0.05;
        double prev = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
        for (int n = 1; n <= 50; n++) {
            double curr = DecayCalculator.computePn(epsilon, iota, p0, lambda, n);
            checkBool("p(" + n + ") < p(" + (n - 1) + ")", curr < prev);
            prev = curr;
        }
        checkBool("p(50) > 0", prev > 0);
    }

    static void testTrimHistory() {
        System.out.println("\n--- Trim history ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            history.add(new DecayCalculator.PeriodRecord(i, i, now.minusDays(50 - i)));
        }
        List<DecayCalculator.PeriodRecord> trimmed = DecayCalculator.trimHistory(history, 30);
        checkBool("Trimmed to 30 entries", trimmed.size() == 30);
        checkBool("Kept most recent (sellTimes=49)", trimmed.get(trimmed.size() - 1).sellTimes == 49);
        checkBool("Oldest is entry 20", trimmed.get(0).sellTimes == 20);

        List<DecayCalculator.PeriodRecord> small = new ArrayList<>();
        small.add(new DecayCalculator.PeriodRecord(1, 1, now));
        List<DecayCalculator.PeriodRecord> notTrimmed = DecayCalculator.trimHistory(small, 30);
        checkBool("No trim when under limit", notTrimmed.size() == 1);
    }

    static void testTrimHistoryEdgeCases() {
        System.out.println("\n--- Trim history edge cases ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);

        List<DecayCalculator.PeriodRecord> empty = new ArrayList<>();
        List<DecayCalculator.PeriodRecord> result = DecayCalculator.trimHistory(empty, 30);
        checkBool("Empty list stays empty", result.isEmpty());

        List<DecayCalculator.PeriodRecord> exact = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            exact.add(new DecayCalculator.PeriodRecord(i, i, now.minusDays(30 - i)));
        }
        result = DecayCalculator.trimHistory(exact, 30);
        checkBool("Exact size, no trim", result.size() == 30);

        List<DecayCalculator.PeriodRecord> oneOver = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            oneOver.add(new DecayCalculator.PeriodRecord(i, i, now.minusDays(31 - i)));
        }
        result = DecayCalculator.trimHistory(oneOver, 30);
        checkBool("1 over limit, trimmed to 30", result.size() == 30);

        List<DecayCalculator.PeriodRecord> single = new ArrayList<>();
        single.add(new DecayCalculator.PeriodRecord(1, 1, now));
        result = DecayCalculator.trimHistory(single, 1);
        checkBool("maxSize=1, single entry kept", result.size() == 1);

        List<DecayCalculator.PeriodRecord> two = new ArrayList<>();
        two.add(new DecayCalculator.PeriodRecord(1, 1, now.minusDays(1)));
        two.add(new DecayCalculator.PeriodRecord(2, 2, now));
        result = DecayCalculator.trimHistory(two, 1);
        checkBool("maxSize=1, keeps most recent", result.size() == 1);
        checkBool("maxSize=1, kept entry has sellTimes=2", result.get(0).sellTimes == 2);
    }

    static void testArticleFullFormula() {
        System.out.println("\n--- Article full formula integration ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double lambda = 0.05;
        double delta = 1.0;
        double tauDays = 7.0;
        double p0 = 100;
        double epsilon = 0.95;
        double iota = 1.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 20, now.minusDays(1)));
        history.add(new DecayCalculator.PeriodRecord(0, 15, now.minusDays(8)));
        history.add(new DecayCalculator.PeriodRecord(0, 30, now.minusDays(15)));

        double nDecayed = DecayCalculator.computeDecayedFromHistory(history, 5, now.minusHours(2), true, delta, tauDays, now);
        checkBool("n(t) > 0", nDecayed > 0);

        double pn = DecayCalculator.computePn(epsilon, iota, p0, lambda, nDecayed);
        checkBool("p(n) > 0", pn > 0);
        checkBool("p(n) < p0", pn < p0);

        double mn = DecayCalculator.computeMnTheoretical(lambda, nDecayed);
        checkBool("M(n) > 0", mn > 0);

        double pn0 = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
        checkBool("p(0) > p(n)", pn0 > pn);

        double rounded = Math.round(pn * 100.0) / 100.0;
        checkBool("Rounded price is finite", Double.isFinite(rounded));
        checkBool("Rounded price >= 0.01", rounded >= 0.01 || pn < 0.01);
    }

    static void testArticleFormulaConsistency() {
        System.out.println("\n--- Article formula consistency ---");
        double lambda = 0.05;
        double p0 = 100;
        double epsilon = 0.95;
        double iota = 1.0;

        double mn10 = DecayCalculator.computeMnTheoretical(lambda, 10);
        double pn10 = DecayCalculator.computePn(epsilon, iota, p0, lambda, 10);
        double pn0 = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
        checkBool("M(10) * lambda < 1 (bounded)", mn10 * lambda < 1.0);
        check("p(0) = epsilon * iota * p0", pn0, epsilon * iota * p0, 0.001);
        check("p(10) = round(epsilon * iota * p0 * e^(-lambda*10), 2)", pn10, Math.round(epsilon * iota * p0 * Math.exp(-lambda * 10) * 100.0) / 100.0, 0.001);

        double q = DecayCalculator.computeQuota(10, 2, 5, 100);
        check("Q = (Q_B + gamma*P) * T", q, (10 + 2 * 5) * 100, 0.001);

        double eps = DecayCalculator.computeEpsilon(46, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 1.0, 0);
        double holidayImpact = DecayCalculator.holidayImpact(0.15, 46, 65536, 46);
        check("epsilon = (1 - holidayImpact) * beta + noise", eps, (1 - holidayImpact) * 1.0 + 0, 0.001);

        double n0 = 100;
        double delta = 1.0;
        double tauDays = 7.0;
        double elapsedDays = 7.0;
        double decayed = Math.floor(n0 / (Math.exp(delta * (elapsedDays - tauDays)) + 1));
        check("n(t) = FLOOR(n0 / (exp(delta*(t-tau)) + 1))", decayed, 50.0, 0.5);
    }

    static void testNumericalOverflowUnderflow() {
        System.out.println("\n--- Numerical overflow/underflow ---");
        double pn = DecayCalculator.computePn(1, 1, Double.MAX_VALUE, 0.1, 0);
        checkBool("p0=MAX_VALUE, n=0 => very large finite", Double.isFinite(pn) && pn > 0);

        pn = DecayCalculator.computePn(1, 1, Double.MAX_VALUE, 0.1, 1000);
        checkBool("p0=MAX_VALUE, large n => finite or 0", Double.isFinite(pn) || pn == 0);

        double mn = DecayCalculator.computeMnTheoretical(0.1, Double.MAX_VALUE);
        checkBool("M(n) with n=MAX_VALUE, finite", Double.isFinite(mn));

        mn = DecayCalculator.computeMnTheoretical(Double.MAX_VALUE, 1);
        checkBool("M(n) with lambda=MAX_VALUE, finite", Double.isFinite(mn));

        double impact = DecayCalculator.holidayImpact(0.15, 46, Double.MIN_VALUE, 46);
        checkBool("Holiday impact with tiny sigma, finite", Double.isFinite(impact));

        double days = DecayCalculator.computeDecayHistoryDays(7, Double.MIN_VALUE, 100);
        checkBool("Decay history days with tiny delta, finite", Double.isFinite(days));

        checkNoThrow("computeDecayedFromHistory with MAX_VALUE delta", () -> {
            LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
            List<DecayCalculator.PeriodRecord> h = new ArrayList<>();
            h.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(7)));
            DecayCalculator.computeDecayedFromHistory(h, 0, null, true, Double.MAX_VALUE, 7.0, now);
        });

        checkNoThrow("computeDecayedFromHistory with MIN_VALUE delta", () -> {
            LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
            List<DecayCalculator.PeriodRecord> h = new ArrayList<>();
            h.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(7)));
            DecayCalculator.computeDecayedFromHistory(h, 0, null, true, Double.MIN_VALUE, 7.0, now);
        });
    }

    static void testConcurrentAccess() {
        System.out.println("\n--- Concurrent access ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < 30; i++) {
            history.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(i + 1)));
        }

        int threadCount = 8;
        int iterationsPerThread = 10000;
        AtomicInteger errorCount = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        double result = DecayCalculator.computeDecayedFromHistory(
                                history, 50, now.minusHours(1), true, delta, tauDays, now);
                        if (Double.isNaN(result) || Double.isInfinite(result) || result < 0) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            checkBool("Concurrent decay calc completed in time", completed);
            checkBool("Concurrent decay calc no errors", errorCount.get() == 0);
        } catch (InterruptedException e) {
            checkBool("Concurrent test interrupted", false);
        }

        AtomicInteger epsilonErrors = new AtomicInteger(0);
        CountDownLatch epsilonLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        double result = DecayCalculator.computeEpsilon(
                                200, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 0.98, 0);
                        if (Double.isNaN(result) || Double.isInfinite(result)) {
                            epsilonErrors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    epsilonErrors.incrementAndGet();
                } finally {
                    epsilonLatch.countDown();
                }
            }).start();
        }

        try {
            boolean completed = epsilonLatch.await(30, TimeUnit.SECONDS);
            checkBool("Concurrent epsilon calc completed in time", completed);
            checkBool("Concurrent epsilon calc no errors", epsilonErrors.get() == 0);
        } catch (InterruptedException e) {
            checkBool("Concurrent epsilon test interrupted", false);
        }

        AtomicInteger trimErrors = new AtomicInteger(0);
        CountDownLatch trimLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        List<DecayCalculator.PeriodRecord> localHistory = new ArrayList<>();
                        for (int j = 0; j < 50; j++) {
                            localHistory.add(new DecayCalculator.PeriodRecord(j, j, now.minusDays(50 - j)));
                        }
                        List<DecayCalculator.PeriodRecord> trimmed = DecayCalculator.trimHistory(localHistory, 30);
                        if (trimmed.size() != 30) {
                            trimErrors.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    trimErrors.incrementAndGet();
                } finally {
                    trimLatch.countDown();
                }
            }).start();
        }

        try {
            boolean completed = trimLatch.await(30, TimeUnit.SECONDS);
            checkBool("Concurrent trimHistory completed in time", completed);
            checkBool("Concurrent trimHistory no errors", trimErrors.get() == 0);
        } catch (InterruptedException e) {
            checkBool("Concurrent trim test interrupted", false);
        }
    }

    static void testPerformance() {
        System.out.println("\n--- Performance tests ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> largeHistory = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            largeHistory.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(i + 1)));
        }

        long start = System.nanoTime();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computeDecayedFromHistory(largeHistory, 50, now.minusHours(1), true, delta, tauDays, now);
        }
        long elapsed = System.nanoTime() - start;
        double msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  30-period history, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Decay calc < 0.1ms/call", msPerCall < 0.1);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computeEpsilon(200, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 0.98, 0);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  Epsilon calc, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Epsilon calc < 0.01ms/call", msPerCall < 0.01);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computeMnTheoretical(0.05, 100);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  M(n) calc, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("M(n) calc < 0.01ms/call", msPerCall < 0.01);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computePn(0.95, 1.0, 100, 0.05, 50);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  p(n) calc, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("p(n) calc < 0.01ms/call", msPerCall < 0.01);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computeQuota(10, 2, 5, 100);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  Quota calc, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Quota calc < 0.01ms/call", msPerCall < 0.01);

        List<DecayCalculator.PeriodRecord> hugeHistory = new ArrayList<>();
        Random rng = new Random(42);
        for (int i = 0; i < 1000; i++) {
            hugeHistory.add(new DecayCalculator.PeriodRecord(rng.nextInt(50), rng.nextInt(50), now.minusDays(i + 1)));
        }
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            DecayCalculator.computeDecayedFromHistory(hugeHistory, 50, now.minusHours(1), true, delta, tauDays, now);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / 1000;
        System.out.println("  1000-period history, 1k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("1000-period decay < 1ms/call", msPerCall < 1.0);

        List<DecayCalculator.PeriodRecord> trimHistory = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            trimHistory.add(new DecayCalculator.PeriodRecord(i, i, now.minusDays(100 - i)));
        }
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.trimHistory(trimHistory, 30);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  Trim history, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Trim history < 0.05ms/call", msPerCall < 0.05);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.holidayImpact(0.15, 46, 65536, 200);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  Holiday impact, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Holiday impact < 0.01ms/call", msPerCall < 0.01);

        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            DecayCalculator.computeDecayHistoryDays(7, 1, 100);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / iterations;
        System.out.println("  Decay history days, 100k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Decay history days < 0.01ms/call", msPerCall < 0.01);

        System.out.println("\n  --- Full pipeline performance ---");
        List<DecayCalculator.PeriodRecord> pipelineHistory = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            pipelineHistory.add(new DecayCalculator.PeriodRecord(0, 100, now.minusDays(i + 1)));
        }
        start = System.nanoTime();
        int pipelineIterations = 10000;
        for (int i = 0; i < pipelineIterations; i++) {
            double n = DecayCalculator.computeDecayedFromHistory(pipelineHistory, 50, now.minusHours(1), true, delta, tauDays, now);
            double eps = DecayCalculator.computeEpsilon(200, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 0.98, 0);
            double price = DecayCalculator.computePn(eps, 1.0, 100, 0.05, n);
            double revenue = DecayCalculator.computeMnTheoretical(0.05, n);
            double quota = DecayCalculator.computeQuota(10, 2, 5, 100);
        }
        elapsed = System.nanoTime() - start;
        msPerCall = elapsed / 1_000_000.0 / pipelineIterations;
        System.out.println("  Full pipeline (decay+epsilon+pn+mn+quota), 10k calls: " + String.format("%.4f", msPerCall) + " ms/call");
        checkBool("Full pipeline < 0.5ms/call", msPerCall < 0.5);
    }
}
