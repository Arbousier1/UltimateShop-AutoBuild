package cn.superiormc.ultimateshop.utils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DecayCalculator {

    private static final double SECONDS_PER_DAY = 86400.0;

    public static class PeriodRecord {
        public final int buyTimes;
        public final int sellTimes;
        public final LocalDateTime resetTime;

        public PeriodRecord(int buyTimes, int sellTimes, LocalDateTime resetTime) {
            this.buyTimes = buyTimes;
            this.sellTimes = sellTimes;
            this.resetTime = resetTime;
        }
    }

    public static double computeDecayedFromHistory(
            List<PeriodRecord> history,
            int currentTimes,
            LocalDateTime currentResetTime,
            boolean isSell,
            double delta,
            double tauDays,
            LocalDateTime now) {
        double totalDecayed = 0;
        for (PeriodRecord record : history) {
            int n0 = isSell ? record.sellTimes : record.buyTimes;
            if (n0 <= 0) continue;
            double elapsedDays = 0;
            if (record.resetTime != null) {
                elapsedDays = ChronoUnit.SECONDS.between(record.resetTime, now) / SECONDS_PER_DAY;
            }
            double decayedValue = Math.floor(n0 / (Math.exp(delta * (elapsedDays - tauDays)) + 1));
            totalDecayed += decayedValue;
        }
        if (currentTimes > 0) {
            if (currentResetTime != null) {
                double elapsedDays = ChronoUnit.SECONDS.between(currentResetTime, now) / SECONDS_PER_DAY;
                double currentDecayed = Math.floor(currentTimes / (Math.exp(delta * (elapsedDays - tauDays)) + 1));
                totalDecayed += currentDecayed;
            } else {
                totalDecayed += currentTimes;
            }
        }
        return totalDecayed;
    }

    public static double computeEpsilon(int dayOfYear,
                                         double alphaWinter, double muWinter, double sigmaWinter,
                                         double alphaSummer, double muSummer, double sigmaSummer,
                                         double alphaNational, double muNational, double sigmaNational,
                                         double beta, double noise) {
        double holidayImpact = holidayImpact(alphaWinter, muWinter, sigmaWinter, dayOfYear)
                + holidayImpact(alphaSummer, muSummer, sigmaSummer, dayOfYear)
                + holidayImpact(alphaNational, muNational, sigmaNational, dayOfYear);
        return (1 - holidayImpact) * beta + noise;
    }

    public static double holidayImpact(double alpha, double mu, double sigma, int dayOfYear) {
        if (sigma <= 0 || alpha <= 0) return 0;
        double offset = dayOfYear - mu;
        double sigmaSq = sigma * sigma;
        if (sigmaSq == 0) return 0;
        double exponent = -Math.pow(offset, 6) / (2 * sigmaSq);
        if (Double.isInfinite(exponent)) return 0;
        double result = alpha * Math.exp(exponent);
        if (Double.isNaN(result) || Double.isInfinite(result)) return 0;
        return result;
    }

    public static double computeQuota(double quotaBase, double activeWeight, double activeHours, double totalQuota) {
        return (quotaBase + activeWeight * activeHours) * totalQuota;
    }

    public static double computeDecayHistoryDays(double tauDays, double delta, int periodSellLimit) {
        if (periodSellLimit <= 1 || delta <= 0) return 0;
        double result = tauDays + Math.log(periodSellLimit - 1) / delta;
        if (Double.isNaN(result) || Double.isInfinite(result)) return 0;
        return result;
    }

    public static double computeMnTheoretical(double lambda, double n) {
        if (lambda <= 0) return 0;
        return (1 - Math.exp(-lambda * n)) / lambda;
    }

    public static double computePn(double epsilon, double iota, double p0, double lambda, double n) {
        double raw = epsilon * iota * p0 * Math.exp(-lambda * n);
        double rounded = Math.round(raw * 100.0) / 100.0;
        return Math.max(rounded, 0.01);
    }

    public static List<PeriodRecord> trimHistory(List<PeriodRecord> history, int maxSize) {
        if (history.size() <= maxSize) return history;
        return new ArrayList<>(history.subList(history.size() - maxSize, history.size()));
    }
}
