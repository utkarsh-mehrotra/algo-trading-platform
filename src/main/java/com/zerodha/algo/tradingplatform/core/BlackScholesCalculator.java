package com.zerodha.algo.tradingplatform.core;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// Requires commons-math3
@Component
public class BlackScholesCalculator {

    private final NormalDistribution normalDistribution = new NormalDistribution();

    public Greeks calculateOptionsGreeks(
            double S, // Spot price
            double K, // Strike price
            double T, // Time to maturity in years
            double r, // Risk-free rate (e.g. 0.05 for 5%)
            double v, // Volatility (implied)
            boolean isCall) {

        if (T <= 0 || v <= 0) {
            return new Greeks(0, 0, 0, 0); // Expired or invalid
        }

        double d1 = (Math.log(S / K) + (r + v * v / 2) * T) / (v * Math.sqrt(T));
        double d2 = d1 - v * Math.sqrt(T);

        double delta, theta;
        double gamma = normalDistribution.density(d1) / (S * v * Math.sqrt(T));
        double vega = S * normalDistribution.density(d1) * Math.sqrt(T);

        if (isCall) {
            delta = normalDistribution.cumulativeProbability(d1);
            theta = -(S * normalDistribution.density(d1) * v) / (2 * Math.sqrt(T))
                    - r * K * Math.exp(-r * T) * normalDistribution.cumulativeProbability(d2);
        } else {
            delta = normalDistribution.cumulativeProbability(d1) - 1;
            theta = -(S * normalDistribution.density(d1) * v) / (2 * Math.sqrt(T))
                    + r * K * Math.exp(-r * T) * normalDistribution.cumulativeProbability(-d2);
        }

        return new Greeks(delta, gamma, theta, vega);
    }

    public static double getDaysToMaturityInYears(LocalDate currentDate, LocalDate expiryDate) {
        long days = ChronoUnit.DAYS.between(currentDate, expiryDate);
        return Math.max(days, 0) / 365.0;
    }

    public record Greeks(double delta, double gamma, double theta, double vega) {}
}
