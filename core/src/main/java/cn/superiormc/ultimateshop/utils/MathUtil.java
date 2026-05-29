package cn.superiormc.ultimateshop.utils;

import cn.superiormc.ultimateshop.managers.ConfigManager;
import cn.superiormc.ultimateshop.managers.ErrorManager;
import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;
import com.ezylang.evalex.parser.Token;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

public class MathUtil {

    public static int scale;

    public static DecimalFormat integerFormat;

    public static DecimalFormat decimalFormat;

    private static ExpressionConfiguration expressionConfig;

    private static final int SIGMA_MAX_ITERATIONS = 100000;

    public static void init() {
        scale = ConfigManager.configManager.getInt("math.scale", 2);
        String integerPattern = ConfigManager.configManager.getString("number-display.format.integer", "#,##0");
        String decimalPattern = ConfigManager.configManager.getString("number-display.format.decimal", "#,##0.00##########");
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        integerFormat = new DecimalFormat(integerPattern, symbols);
        decimalFormat = new DecimalFormat(decimalPattern, symbols);

        expressionConfig = ExpressionConfiguration.defaultConfiguration()
                .withAdditionalFunctions(Map.entry("SIGMA", new SigmaFunction()));
    }

    public static double multiply(double left, double right) {
        return BigDecimal.valueOf(left).multiply(BigDecimal.valueOf(right)).doubleValue();
    }

    public static String toDisplayString(double value) {
        return toDisplayString(BigDecimal.valueOf(value));
    }

    public static String toDisplayString(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal working = ConfigManager.configManager.getBoolean("number-display.strip-trailing-zeros.enabled") ? value.stripTrailingZeros() : value;
        if (!ConfigManager.configManager.getBoolean("number-display.format.enabled")) {
            return value.setScale(Math.max(0, value.scale()), RoundingMode.HALF_UP).toPlainString();
        }
        return working.scale() <= 0
                ? integerFormat.format(working)
                : decimalFormat.format(working);
    }

    public static BigDecimal doCalculate(String mathStr) {
        return doCalculate(mathStr, scale);
    }

    public static BigDecimal doCalculate(String mathStr, int scale) {
        try {
            if (!ConfigManager.configManager.getBoolean("math.enabled")) {
                return new BigDecimal(mathStr);
            }
            return new Expression(mathStr, expressionConfig).evaluate()
                    .getNumberValue()
                    .setScale(scale, RoundingMode.HALF_UP);
        } catch (Throwable throwable) {
            if (ConfigManager.configManager.getBoolean("debug")) {
                throwable.printStackTrace();
            }
            ErrorManager.errorManager.sendErrorMessage("§cError: Your number option value " +
                    mathStr + " can not be read as a number, maybe " +
                    "set math.enabled to false in config.yml maybe solve this problem!");
            return BigDecimal.ZERO;
        }
    }

    @FunctionParameter(name = "start")
    @FunctionParameter(name = "end")
    @FunctionParameter(name = "body")
    public static class SigmaFunction extends AbstractFunction {

        @Override
        public EvaluationValue evaluate(Expression expression, Token functionToken,
                                         EvaluationValue... parameterValues)
                throws EvaluationException {
            int start = parameterValues[0].getNumberValue().intValue();
            int end = parameterValues[1].getNumberValue().intValue();
            String body = parameterValues[2].getStringValue();

            if (start > end) {
                return EvaluationValue.numberValue(BigDecimal.ZERO);
            }

            int count = end - start + 1;
            if (count > SIGMA_MAX_ITERATIONS) {
                throw new EvaluationException(functionToken,
                        "SIGMA: too many iterations (" + count + "), maximum is " + SIGMA_MAX_ITERATIONS);
            }

            BigDecimal sum = BigDecimal.ZERO;
            for (int i = start; i <= end; i++) {
                Expression subExpr = new Expression(body, expressionConfig)
                        .with("i", BigDecimal.valueOf(i));
                try {
                    sum = sum.add(subExpr.evaluate().getNumberValue());
                } catch (com.ezylang.evalex.parser.ParseException e) {
                    throw new EvaluationException(functionToken,
                            "SIGMA: error in body expression: " + e.getMessage());
                }
            }
            return EvaluationValue.numberValue(sum);
        }
    }
}
