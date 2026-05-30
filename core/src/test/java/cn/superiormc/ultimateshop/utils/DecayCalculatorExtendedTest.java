package cn.superiormc.ultimateshop.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DecayCalculatorExtendedTest {

    private static int passed;
    private static int failed;
    private static final double SECONDS_PER_DAY = 86400.0;

    static void check(String name, double actual, double expected, double tolerance) {
        if (Double.isNaN(actual) && Double.isNaN(expected)) {
            passed++;
            System.out.println("  PASS: " + name + " = NaN");
            return;
        }
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

    static void checkBool(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS: " + name);
        } else {
            failed++;
            System.out.println("  FAIL: " + name);
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
        System.out.println("= DecayCalculator Extended Tests (Property-Based + Simulation) =\n");

        testDecayNonNegativity();
        testDecayMonotonicityOverTime();
        testDecayAdditivity();
        testDecayCurrentPlusHistory();
        testHolidayImpactBounded();
        testHolidayImpactMaxAtMu();
        testEpsilonHolidayImpactAdditive();
        testEpsilonLinearInBeta();
        testEpsilonLinearInNoise();
        testMnTheoreticalBounded();
        testPnExponentialDecay();
        testPnLinearInEpsilon();
        testPnLinearInP0();
        testQuotaLinearInAllInputs();
        testTrimPreservesOrder();
        testDecayHistoryDaysMonotonicInNu();
        testRandomStressDecay();
        testRandomStressEpsilon();
        testRandomStressPn();
        testRandomStressMn();
        testSimulationDailyPrice();
        testSimulationWeeklyReset();
        testSimulationYearlyEpsilon();
        testSimulationFullEconomy();
        testArticleSigmaFormula();
        testArticleDecayFormula();
        testArticlePriceFormula();
        testArticleQuotaFormula();
        testArticleRevenueFormula();
        testFuzzAllFunctions();

        System.out.println("\n" + "=".repeat(55));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED!");
        }
    }

    static void testDecayNonNegativity() {
        System.out.println("--- Invariant: decay result is always >= 0 ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        Random rng = new Random(12345);
        for (int i = 0; i < 1000; i++) {
            List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
            int histSize = rng.nextInt(20);
            for (int j = 0; j < histSize; j++) {
                int buy = rng.nextInt(200);
                int sell = rng.nextInt(200);
                LocalDateTime rt = now.minusDays(rng.nextInt(100));
                history.add(new DecayCalculator.PeriodRecord(buy, sell, rng.nextBoolean() ? rt : null));
            }
            int currentTimes = rng.nextInt(200);
            LocalDateTime currentReset = rng.nextBoolean() ? now.minusDays(rng.nextInt(100)) : null;
            boolean isSell = rng.nextBoolean();
            double delta = rng.nextDouble() * 10;
            double tauDays = rng.nextDouble() * 30;
            double result = DecayCalculator.computeDecayedFromHistory(history, currentTimes, currentReset, isSell, delta, tauDays, now);
            checkBool("Random decay >= 0 (#" + i + ")", result >= 0);
            if (result < 0) break;
        }
    }

    static void testDecayMonotonicityOverTime() {
        System.out.println("\n--- Invariant: decayed count decreases as more time passes (recovery) ---");
        LocalDateTime baseTime = LocalDateTime.of(2026, 5, 1, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;
        int n0 = 100;

        double prevDecayed = Double.MAX_VALUE;
        for (int day = 0; day <= 30; day++) {
            LocalDateTime now = baseTime.plusDays(day);
            List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
            history.add(new DecayCalculator.PeriodRecord(0, n0, baseTime));
            double decayed = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
            if (prevDecayed < Double.MAX_VALUE) {
                checkBool("Decay at day " + day + " <= day " + (day - 1) + " (recovery)", decayed <= prevDecayed + 0.5);
            }
            prevDecayed = decayed;
        }
    }

    static void testDecayAdditivity() {
        System.out.println("\n--- Invariant: decay(A+B) = decay(A) + decay(B) ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> histA = new ArrayList<>();
        histA.add(new DecayCalculator.PeriodRecord(0, 50, now.minusDays(3)));
        double decayA = DecayCalculator.computeDecayedFromHistory(histA, 0, null, true, delta, tauDays, now);

        List<DecayCalculator.PeriodRecord> histB = new ArrayList<>();
        histB.add(new DecayCalculator.PeriodRecord(0, 30, now.minusDays(5)));
        double decayB = DecayCalculator.computeDecayedFromHistory(histB, 0, null, true, delta, tauDays, now);

        List<DecayCalculator.PeriodRecord> histAB = new ArrayList<>();
        histAB.add(new DecayCalculator.PeriodRecord(0, 50, now.minusDays(3)));
        histAB.add(new DecayCalculator.PeriodRecord(0, 30, now.minusDays(5)));
        double decayAB = DecayCalculator.computeDecayedFromHistory(histAB, 0, null, true, delta, tauDays, now);

        check("Decay(A+B) = Decay(A) + Decay(B)", decayAB, decayA + decayB, 0.01);

        List<DecayCalculator.PeriodRecord> histC = new ArrayList<>();
        histC.add(new DecayCalculator.PeriodRecord(0, 20, now.minusDays(10)));
        double decayC = DecayCalculator.computeDecayedFromHistory(histC, 0, null, true, delta, tauDays, now);

        List<DecayCalculator.PeriodRecord> histABC = new ArrayList<>();
        histABC.addAll(histAB);
        histABC.addAll(histC);
        double decayABC = DecayCalculator.computeDecayedFromHistory(histABC, 0, null, true, delta, tauDays, now);

        check("Decay(A+B+C) = Decay(A) + Decay(B) + Decay(C)", decayABC, decayA + decayB + decayC, 0.01);
    }

    static void testDecayCurrentPlusHistory() {
        System.out.println("\n--- Invariant: current period + history = sum of both ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        double delta = 1.0;
        double tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        history.add(new DecayCalculator.PeriodRecord(0, 40, now.minusDays(3)));

        double histOnly = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, now);
        double currentOnly = DecayCalculator.computeDecayedFromHistory(new ArrayList<>(), 20, now.minusHours(6), true, delta, tauDays, now);
        double combined = DecayCalculator.computeDecayedFromHistory(history, 20, now.minusHours(6), true, delta, tauDays, now);

        check("History + current = combined", combined, histOnly + currentOnly, 0.01);
    }

    static void testHolidayImpactBounded() {
        System.out.println("\n--- Invariant: 0 <= holidayImpact <= alpha ---");
        Random rng = new Random(54321);
        for (int i = 0; i < 500; i++) {
            double alpha = rng.nextDouble() * 0.5;
            double mu = rng.nextInt(366);
            double sigma = rng.nextDouble() * 100000 + 0.001;
            int day = rng.nextInt(400) - 10;
            double impact = DecayCalculator.holidayImpact(alpha, mu, sigma, day);
            checkBool("Impact in [0, alpha] (#" + i + ")", impact >= 0 && impact <= alpha + 1e-12);
            if (impact < 0 || impact > alpha + 1e-12) break;
        }
    }

    static void testHolidayImpactMaxAtMu() {
        System.out.println("\n--- Invariant: holidayImpact is maximized at day=mu ---");
        double alpha = 0.15;
        double[] mus = {46, 100, 200, 274, 365};
        double[] sigmas = {65536, 100, 33.1776, 10, 1};
        for (int i = 0; i < mus.length; i++) {
            double mu = mus[i];
            double sigma = sigmas[i];
            double impactAtMu = DecayCalculator.holidayImpact(alpha, mu, sigma, (int) mu);
            double impactBefore = DecayCalculator.holidayImpact(alpha, mu, sigma, (int) mu - 5);
            double impactAfter = DecayCalculator.holidayImpact(alpha, mu, sigma, (int) mu + 5);
            checkBool("Impact at mu >= impact at mu-5 (mu=" + (int) mu + ")", impactAtMu >= impactBefore - 1e-15);
            checkBool("Impact at mu >= impact at mu+5 (mu=" + (int) mu + ")", impactAtMu >= impactAfter - 1e-15);
        }
    }

    static void testEpsilonHolidayImpactAdditive() {
        System.out.println("\n--- Invariant: epsilon with holidays = (1 - sum(impacts)) * beta + noise ---");
        int day = 46;
        double alphaW = 0.15, muW = 46, sigmaW = 65536;
        double alphaS = 0.10, muS = 213, sigmaS = 65536;
        double alphaN = 0.075, muN = 274, sigmaN = 33.1776;
        double beta = 0.98;
        double noise = 0.02;

        double impactW = DecayCalculator.holidayImpact(alphaW, muW, sigmaW, day);
        double impactS = DecayCalculator.holidayImpact(alphaS, muS, sigmaS, day);
        double impactN = DecayCalculator.holidayImpact(alphaN, muN, sigmaN, day);
        double totalImpact = impactW + impactS + impactN;
        double expectedEps = (1 - totalImpact) * beta + noise;

        double actualEps = DecayCalculator.computeEpsilon(day, alphaW, muW, sigmaW, alphaS, muS, sigmaS, alphaN, muN, sigmaN, beta, noise);
        check("Epsilon = (1 - sum(holidayImpact)) * beta + noise", actualEps, expectedEps, 0.0001);
    }

    static void testEpsilonLinearInBeta() {
        System.out.println("\n--- Invariant: epsilon is linear in beta ---");
        double eps1 = DecayCalculator.computeEpsilon(100, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 1.0, 0);
        double eps098 = DecayCalculator.computeEpsilon(100, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 0.98, 0);
        double eps050 = DecayCalculator.computeEpsilon(100, 0.15, 46, 65536, 0, 0, 0, 0, 0, 0, 0.50, 0);

        double holidayImpact = DecayCalculator.holidayImpact(0.15, 46, 65536, 100);
        check("eps(1.0) = (1 - impact) * 1.0", eps1, (1 - holidayImpact) * 1.0, 0.001);
        check("eps(0.98) = (1 - impact) * 0.98", eps098, (1 - holidayImpact) * 0.98, 0.001);
        check("eps(0.50) = (1 - impact) * 0.50", eps050, (1 - holidayImpact) * 0.50, 0.001);
        check("eps(0.98) / eps(1.0) = 0.98", eps098 / eps1, 0.98, 0.001);
    }

    static void testEpsilonLinearInNoise() {
        System.out.println("\n--- Invariant: epsilon is linear in noise ---");
        double eps0 = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0);
        double eps01 = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0.1);
        double eps02 = DecayCalculator.computeEpsilon(100, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0.2);

        check("eps(noise=0) = 1.0", eps0, 1.0, 0.001);
        check("eps(noise=0.1) = 1.1", eps01, 1.1, 0.001);
        check("eps(noise=0.2) = 1.2", eps02, 1.2, 0.001);
        check("eps(noise=0.2) - eps(noise=0.1) = 0.1", eps02 - eps01, 0.1, 0.001);
    }

    static void testMnTheoreticalBounded() {
        System.out.println("\n--- Invariant: 0 <= M(n) <= n for all n >= 0 ---");
        double lambda = 0.1;
        for (int n = 0; n <= 200; n += 5) {
            double mn = DecayCalculator.computeMnTheoretical(lambda, n);
            checkBool("M(" + n + ") >= 0", mn >= 0);
            checkBool("M(" + n + ") <= " + n, mn <= n + 1e-10);
        }

        for (double lam : new double[]{0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0}) {
            double mn = DecayCalculator.computeMnTheoretical(lam, 100);
            checkBool("M(100) with lambda=" + lam + " <= 100", mn <= 100 + 1e-10);
            checkBool("M(100) with lambda=" + lam + " <= 1/lambda=" + (1.0 / lam), mn <= 1.0 / lam + 1e-10);
        }
    }

    static void testPnExponentialDecay() {
        System.out.println("\n--- Invariant: p(n) / p(0) = exp(-lambda*n) ---");
        double epsilon = 0.95;
        double iota = 1.1;
        double p0 = 100;
        double lambda = 0.05;

        for (int n : new int[]{1, 5, 10, 20, 50, 100}) {
            double pn = DecayCalculator.computePn(epsilon, iota, p0, lambda, n);
            double p0val = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
            double ratio = pn / p0val;
            double expected = Math.exp(-lambda * n);
            check("p(" + n + ")/p(0) = exp(-lambda*" + n + ")", ratio, expected, 0.001);
        }
    }

    static void testPnLinearInEpsilon() {
        System.out.println("\n--- Invariant: p(n) is linear in epsilon ---");
        double iota = 1.0, p0 = 100, lambda = 0.05;
        int n = 10;
        double p1 = DecayCalculator.computePn(1.0, iota, p0, lambda, n);
        double p05 = DecayCalculator.computePn(0.5, iota, p0, lambda, n);
        double p02 = DecayCalculator.computePn(0.2, iota, p0, lambda, n);

        check("p(eps=0.5) = 0.5 * p(eps=1.0)", p05, 0.5 * p1, 0.001);
        check("p(eps=0.2) = 0.2 * p(eps=1.0)", p02, 0.2 * p1, 0.001);
    }

    static void testPnLinearInP0() {
        System.out.println("\n--- Invariant: p(n) is linear in p0 ---");
        double epsilon = 0.95, iota = 1.0, lambda = 0.05;
        int n = 10;
        double p100 = DecayCalculator.computePn(epsilon, iota, 100, lambda, n);
        double p200 = DecayCalculator.computePn(epsilon, iota, 200, lambda, n);
        double p50 = DecayCalculator.computePn(epsilon, iota, 50, lambda, n);

        check("p(p0=200) = 2 * p(p0=100)", p200, 2 * p100, 0.001);
        check("p(p0=50) = 0.5 * p(p0=100)", p50, 0.5 * p100, 0.001);
    }

    static void testQuotaLinearInAllInputs() {
        System.out.println("\n--- Invariant: Q is linear in each input ---");
        double q1 = DecayCalculator.computeQuota(10, 2, 5, 100);
        double q2 = DecayCalculator.computeQuota(20, 2, 5, 100);
        check("Q(2*Q_B) - Q(Q_B) = Q_B*T", q2 - q1, 10 * 100, 0.001);

        q1 = DecayCalculator.computeQuota(10, 2, 5, 100);
        q2 = DecayCalculator.computeQuota(10, 4, 5, 100);
        check("Q(2*gamma) - Q(gamma) = gamma*P*T", q2 - q1, 2 * 5 * 100, 0.001);

        q1 = DecayCalculator.computeQuota(10, 2, 5, 100);
        q2 = DecayCalculator.computeQuota(10, 2, 5, 200);
        check("Q(2*T) = 2 * Q(T)", q2, 2 * q1, 0.001);
    }

    static void testTrimPreservesOrder() {
        System.out.println("\n--- Invariant: trimHistory preserves insertion order ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            history.add(new DecayCalculator.PeriodRecord(i, i * 2, now.minusDays(50 - i)));
        }
        List<DecayCalculator.PeriodRecord> trimmed = DecayCalculator.trimHistory(history, 10);
        for (int i = 0; i < trimmed.size() - 1; i++) {
            checkBool("Order preserved: trimmed[" + i + "].sellTimes < trimmed[" + (i + 1) + "].sellTimes",
                    trimmed.get(i).sellTimes < trimmed.get(i + 1).sellTimes);
        }
        checkBool("First trimmed entry has sellTimes=80", trimmed.get(0).sellTimes == 80);
        checkBool("Last trimmed entry has sellTimes=98", trimmed.get(trimmed.size() - 1).sellTimes == 98);
    }

    static void testDecayHistoryDaysMonotonicInNu() {
        System.out.println("\n--- Invariant: T_history is monotonically increasing in nu ---");
        double tau = 7, delta = 1;
        double prev = 0;
        for (int nu = 2; nu <= 200; nu += 5) {
            double days = DecayCalculator.computeDecayHistoryDays(tau, delta, nu);
            checkBool("T_history(nu=" + nu + ") >= T_history(nu=" + (nu - 5) + ")", days >= prev - 1e-10);
            prev = days;
        }
    }

    static void testRandomStressDecay() {
        System.out.println("\n--- Stress: random decay inputs, 10k iterations ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 29, 12, 0);
        Random rng = new Random(42);
        int errors = 0;
        for (int i = 0; i < 10000; i++) {
            List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
            int histSize = rng.nextInt(30);
            for (int j = 0; j < histSize; j++) {
                history.add(new DecayCalculator.PeriodRecord(
                        rng.nextInt(500), rng.nextInt(500),
                        rng.nextBoolean() ? now.minusSeconds(rng.nextLong(86400L * 60)) : null));
            }
            int currentTimes = rng.nextInt(500);
            LocalDateTime currentReset = rng.nextBoolean() ? now.minusSeconds(rng.nextLong(86400L * 60)) : null;
            double delta = rng.nextDouble() * 5 + 0.001;
            double tauDays = rng.nextDouble() * 30 + 1;
            double result = DecayCalculator.computeDecayedFromHistory(history, currentTimes, currentReset, true, delta, tauDays, now);
            if (Double.isNaN(result) || Double.isInfinite(result) || result < 0) errors++;
        }
        checkBool("10k random decay inputs, 0 errors", errors == 0);
    }

    static void testRandomStressEpsilon() {
        System.out.println("\n--- Stress: random epsilon inputs, 10k iterations ---");
        Random rng = new Random(99);
        int errors = 0;
        for (int i = 0; i < 10000; i++) {
            int day = rng.nextInt(366) + 1;
            double alphaW = rng.nextDouble() * 0.3;
            double muW = rng.nextInt(366);
            double sigmaW = rng.nextDouble() * 100000 + 0.01;
            double alphaS = rng.nextDouble() * 0.3;
            double muS = rng.nextInt(366);
            double sigmaS = rng.nextDouble() * 100000 + 0.01;
            double alphaN = rng.nextDouble() * 0.3;
            double muN = rng.nextInt(366);
            double sigmaN = rng.nextDouble() * 100000 + 0.01;
            double beta = rng.nextDouble();
            double noise = rng.nextDouble() * 0.2 - 0.1;
            double result = DecayCalculator.computeEpsilon(day, alphaW, muW, sigmaW, alphaS, muS, sigmaS, alphaN, muN, sigmaN, beta, noise);
            if (Double.isNaN(result) || Double.isInfinite(result)) errors++;
        }
        checkBool("10k random epsilon inputs, 0 NaN/Inf errors", errors == 0);
    }

    static void testRandomStressPn() {
        System.out.println("\n--- Stress: random p(n) inputs, 10k iterations ---");
        Random rng = new Random(777);
        int errors = 0;
        for (int i = 0; i < 10000; i++) {
            double epsilon = rng.nextDouble() * 2 - 0.5;
            double iota = rng.nextDouble() * 2;
            double p0 = rng.nextDouble() * 10000;
            double lambda = rng.nextDouble() * 2;
            double n = rng.nextInt(1000);
            double result = DecayCalculator.computePn(epsilon, iota, p0, lambda, n);
            if (Double.isNaN(result)) errors++;
        }
        checkBool("10k random p(n) inputs, 0 NaN errors", errors == 0);
    }

    static void testRandomStressMn() {
        System.out.println("\n--- Stress: random M(n) inputs, 10k iterations ---");
        Random rng = new Random(314);
        int errors = 0;
        for (int i = 0; i < 10000; i++) {
            double lambda = rng.nextDouble() * 10;
            double n = rng.nextDouble() * 10000;
            double result = DecayCalculator.computeMnTheoretical(lambda, n);
            if (Double.isNaN(result) || Double.isInfinite(result)) errors++;
        }
        checkBool("10k random M(n) inputs, 0 NaN/Inf errors", errors == 0);
    }

    static void testSimulationDailyPrice() {
        System.out.println("\n--- Simulation: daily price over 30 days ---");
        double p0 = 100, lambda = 0.05, epsilon = 0.95, iota = 1.0;
        double prevPrice = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
        for (int day = 1; day <= 30; day++) {
            double pn = DecayCalculator.computePn(epsilon, iota, p0, lambda, day);
            checkBool("Day " + day + ": price decreasing", pn < prevPrice);
            checkBool("Day " + day + ": price > 0", pn > 0);
            checkBool("Day " + day + ": price finite", Double.isFinite(pn));
            prevPrice = pn;
        }
        checkBool("Day 30 price < p0 * 0.5", prevPrice < p0 * 0.5);
        checkBool("Day 30 price > p0 * 0.1", prevPrice > p0 * 0.1);
    }

    static void testSimulationWeeklyReset() {
        System.out.println("\n--- Simulation: weekly reset cycle ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 0, 0);
        double delta = 1.0, tauDays = 7.0;

        List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
        for (int week = 0; week < 4; week++) {
            LocalDateTime resetTime = now.plusWeeks(week);
            LocalDateTime checkTime = resetTime.plusDays(7);
            int sellTimes = 50 + week * 10;
            history.add(new DecayCalculator.PeriodRecord(0, sellTimes, resetTime));
        }

        LocalDateTime finalNow = now.plusWeeks(4);
        double totalDecayed = DecayCalculator.computeDecayedFromHistory(history, 0, null, true, delta, tauDays, finalNow);
        checkBool("4-week history total decayed > 0", totalDecayed > 0);
        checkBool("4-week history total decayed < sum of all sellTimes", totalDecayed < 200);

        double decayedWeek0 = Math.floor(50 / (Math.exp(delta * (28 - tauDays)) + 1));
        double decayedWeek3 = Math.floor(80 / (Math.exp(delta * (7 - tauDays)) + 1));
        checkBool("Most recent week contributes more", decayedWeek3 > decayedWeek0);
    }

    static void testSimulationYearlyEpsilon() {
        System.out.println("\n--- Simulation: epsilon over a full year ---");
        double alphaW = 0.15, muW = 46, sigmaW = 65536;
        double alphaS = 0.15, muS = 213, sigmaS = 65536;
        double alphaN = 0.075, muN = 274, sigmaN = 33.1776;
        double beta = 0.98;

        double minEps = Double.MAX_VALUE;
        double maxEps = Double.MIN_VALUE;
        int minDay = 0, maxDay = 0;

        for (int day = 1; day <= 365; day++) {
            double eps = DecayCalculator.computeEpsilon(day, alphaW, muW, sigmaW, alphaS, muS, sigmaS, alphaN, muN, sigmaN, beta, 0);
            if (eps < minEps) { minEps = eps; minDay = day; }
            if (eps > maxEps) { maxEps = eps; maxDay = day; }
        }

        checkBool("Min epsilon > 0", minEps > 0);
        checkBool("Max epsilon <= beta", maxEps <= beta + 0.001);
        checkBool("Min epsilon at winter (near day 46)", minDay >= 30 && minDay <= 60);
        checkBool("Max epsilon far from holidays", maxDay < 30 || maxDay > 320 || (maxDay > 100 && maxDay < 180));
        System.out.println("  Info: min epsilon=" + String.format("%.4f", minEps) + " at day " + minDay);
        System.out.println("  Info: max epsilon=" + String.format("%.4f", maxEps) + " at day " + maxDay);
    }

    static void testSimulationFullEconomy() {
        System.out.println("\n--- Simulation: full economy model over 7 days ---");
        LocalDateTime now = LocalDateTime.of(2026, 5, 1, 0, 0);
        double p0 = 100, lambda = 0.05, delta = 1.0, tauDays = 7.0;
        double Q_B = 10, gamma = 2, T = 100;
        int dayOfYear = 121;

        double totalRevenue = 0;
        int totalTransactions = 0;

        for (int day = 0; day < 7; day++) {
            LocalDateTime dayStart = now.plusDays(day);
            List<DecayCalculator.PeriodRecord> history = new ArrayList<>();
            for (int d = 0; d < day; d++) {
                history.add(new DecayCalculator.PeriodRecord(0, 10 + d * 2, now.plusDays(d)));
            }

            double nDecayed = DecayCalculator.computeDecayedFromHistory(history, 5, dayStart, true, delta, tauDays, dayStart.plusHours(12));
            double eps = DecayCalculator.computeEpsilon(dayOfYear + day, 0.15, 46, 65536, 0.15, 213, 65536, 0.075, 274, 33.1776, 0.98, 0);
            double price = DecayCalculator.computePn(eps, 1.0, p0, lambda, nDecayed);
            double revenue = DecayCalculator.computeMnTheoretical(lambda, nDecayed);
            double quota = DecayCalculator.computeQuota(Q_B, gamma, day * 4.0, T);

            checkBool("Day " + day + ": price > 0", price > 0);
            checkBool("Day " + day + ": price finite", Double.isFinite(price));
            checkBool("Day " + day + ": revenue > 0", revenue > 0);
            checkBool("Day " + day + ": quota > 0", quota > 0);
            checkBool("Day " + day + ": epsilon in (0, 1)", eps > 0 && eps < 1);

            totalRevenue += revenue;
            totalTransactions += 5;
        }

        checkBool("Total revenue > 0", totalRevenue > 0);
        checkBool("Total transactions > 0", totalTransactions > 0);
        System.out.println("  Info: 7-day total revenue = " + String.format("%.2f", totalRevenue));
    }

    static void testArticleSigmaFormula() {
        System.out.println("\n--- Article formula: sigma_winter = 65536 derivation ---");
        double alpha = 0.15;
        double mu = 46;
        double sigma = 65536;
        double halfWidth = 7;
        double impactAtHalfWidth = DecayCalculator.holidayImpact(alpha, mu, sigma, (int) (mu + halfWidth));
        checkBool("Impact at mu+7 is close to alpha (sigma=65536 is very wide)", Math.abs(impactAtHalfWidth - alpha) / alpha < 0.01);

        double sigmaNarrow = 33.1776;
        double muN = 274;
        double impactAtMu = DecayCalculator.holidayImpact(0.075, muN, sigmaNarrow, (int) muN);
        double impactAtMuPlus7 = DecayCalculator.holidayImpact(0.075, muN, sigmaNarrow, (int) (muN + 7));
        checkBool("Narrow sigma: impact at mu+7 significantly less than at mu", impactAtMuPlus7 < impactAtMu * 0.5);
    }

    static void testArticleDecayFormula() {
        System.out.println("\n--- Article formula: n(t) = FLOOR(n0 / (exp(delta*(t-tau))+1)) ---");
        double delta = 1.0;
        double tau = 7.0;
        int n0 = 100;

        double nAtTau = Math.floor(n0 / (Math.exp(delta * (tau - tau)) + 1));
        check("n(tau) = FLOOR(n0/2) = 50", nAtTau, 50.0, 0.5);

        double nBeforeTau = Math.floor(n0 / (Math.exp(delta * (5 - tau)) + 1));
        checkBool("n(5) > n(tau)", nBeforeTau > nAtTau);

        double nAfterTau = Math.floor(n0 / (Math.exp(delta * (10 - tau)) + 1));
        checkBool("n(10) < n(tau)", nAfterTau < nAtTau);

        double nFarAfter = Math.floor(n0 / (Math.exp(delta * (30 - tau)) + 1));
        checkBool("n(30) ~ 0", nFarAfter < 1);

        double nFarBefore = Math.floor(n0 / (Math.exp(delta * (0 - tau)) + 1));
        checkBool("n(0) ~ n0", nFarBefore > n0 - 2);
    }

    static void testArticlePriceFormula() {
        System.out.println("\n--- Article formula: p(n) = epsilon * iota * p0 * exp(-lambda*n) ---");
        double epsilon = 0.85;
        double iota = 1.25;
        double p0 = 100;
        double lambda = 0.05;

        double p0val = DecayCalculator.computePn(epsilon, iota, p0, lambda, 0);
        check("p(0) = epsilon * iota * p0 = " + (epsilon * iota * p0), p0val, epsilon * iota * p0, 0.001);

        double p10 = DecayCalculator.computePn(epsilon, iota, p0, lambda, 10);
        double expected = epsilon * iota * p0 * Math.exp(-lambda * 10);
        check("p(10) = epsilon * iota * p0 * exp(-0.5)", p10, expected, 0.001);

        double halfLife = Math.log(2) / lambda;
        double pAtHalfLife = DecayCalculator.computePn(epsilon, iota, p0, lambda, halfLife);
        check("p(halfLife) = p(0)/2", pAtHalfLife, p0val / 2, 0.01);
    }

    static void testArticleQuotaFormula() {
        System.out.println("\n--- Article formula: Q = (Q_B + gamma*P) * T ---");
        double Q_B = 10, gamma = 2, P = 5, T = 100;
        double Q = DecayCalculator.computeQuota(Q_B, gamma, P, T);
        check("Q = (10 + 2*5) * 100 = 2000", Q, 2000, 0.001);

        double Q_noActive = DecayCalculator.computeQuota(Q_B, 0, 0, T);
        check("Q with no active time = Q_B * T = 1000", Q_noActive, 1000, 0.001);

        double Q_noBase = DecayCalculator.computeQuota(0, gamma, P, T);
        check("Q with no base = gamma * P * T = 1000", Q_noBase, 1000, 0.001);
    }

    static void testArticleRevenueFormula() {
        System.out.println("\n--- Article formula: M(n) = (1 - exp(-lambda*n)) / lambda ---");
        double lambda = 0.05;

        double M0 = DecayCalculator.computeMnTheoretical(lambda, 0);
        check("M(0) = 0", M0, 0, 0.001);

        double M1 = DecayCalculator.computeMnTheoretical(lambda, 1);
        double expected1 = (1 - Math.exp(-lambda)) / lambda;
        check("M(1) = (1 - exp(-0.05)) / 0.05", M1, expected1, 0.001);

        double Mlimit = 1.0 / lambda;
        double M1000 = DecayCalculator.computeMnTheoretical(lambda, 1000);
        checkBool("M(1000) approaches 1/lambda = 20", Math.abs(M1000 - Mlimit) / Mlimit < 0.001);

        double dM_dn_approx = (DecayCalculator.computeMnTheoretical(lambda, 1.001) - M1) / 0.001;
        double expectedDerivative = Math.exp(-lambda * 1);
        check("dM/dn at n=1 ~ exp(-lambda)", dM_dn_approx, expectedDerivative, 0.01);
    }

    static void testFuzzAllFunctions() {
        System.out.println("\n--- Fuzz: extreme inputs for all public functions ---");
        checkNoThrow("holidayImpact with extreme values", () -> {
            DecayCalculator.holidayImpact(Double.MAX_VALUE, Integer.MAX_VALUE, Double.MAX_VALUE, 1);
            DecayCalculator.holidayImpact(Double.MIN_VALUE, 0, Double.MIN_VALUE, 366);
            DecayCalculator.holidayImpact(0.15, -100, 100, -100);
            DecayCalculator.holidayImpact(0.15, 46, 1e-320, 46);
        });

        checkNoThrow("computeEpsilon with extreme values", () -> {
            DecayCalculator.computeEpsilon(1, Double.MAX_VALUE, 0, Double.MAX_VALUE, 0, 0, 0, 0, 0, 0, Double.MAX_VALUE, Double.MAX_VALUE);
            DecayCalculator.computeEpsilon(366, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            DecayCalculator.computeEpsilon(0, 0.5, 0, 1e-320, 0, 0, 0, 0, 0, 0, 1, 0);
        });

        checkNoThrow("computePn with extreme values", () -> {
            DecayCalculator.computePn(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, 0);
            DecayCalculator.computePn(0, 0, 0, 0, 0);
            DecayCalculator.computePn(-1, -1, -1, -1, -1);
        });

        checkNoThrow("computeMnTheoretical with extreme values", () -> {
            DecayCalculator.computeMnTheoretical(Double.MAX_VALUE, Double.MAX_VALUE);
            DecayCalculator.computeMnTheoretical(Double.MIN_VALUE, 0);
            DecayCalculator.computeMnTheoretical(-1, -1);
        });

        checkNoThrow("computeQuota with extreme values", () -> {
            DecayCalculator.computeQuota(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            DecayCalculator.computeQuota(0, 0, 0, 0);
            DecayCalculator.computeQuota(-1, -1, -1, -1);
        });

        checkNoThrow("computeDecayHistoryDays with extreme values", () -> {
            DecayCalculator.computeDecayHistoryDays(Double.MAX_VALUE, Double.MAX_VALUE, Integer.MAX_VALUE);
            DecayCalculator.computeDecayHistoryDays(0, 0, 0);
            DecayCalculator.computeDecayHistoryDays(-1, -1, -1);
            DecayCalculator.computeDecayHistoryDays(7, Double.MIN_VALUE, 100);
        });

        checkNoThrow("trimHistory with extreme values", () -> {
            DecayCalculator.trimHistory(new ArrayList<>(), 0);
            DecayCalculator.trimHistory(new ArrayList<>(), Integer.MAX_VALUE);
            List<DecayCalculator.PeriodRecord> big = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                big.add(new DecayCalculator.PeriodRecord(i, i, null));
            }
            DecayCalculator.trimHistory(big, 1);
        });
    }
}
