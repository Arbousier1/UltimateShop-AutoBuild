package cn.superiormc.ultimateshop.utils;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.functions.AbstractFunction;
import com.ezylang.evalex.functions.FunctionParameter;
import com.ezylang.evalex.parser.Token;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class MathFunctionTest {

    private static final int SIGMA_MAX_ITERATIONS = 100000;
    private static final ExpressionConfiguration config;
    private static int passed;
    private static int failed;

    static {
        config = ExpressionConfiguration.defaultConfiguration()
                .withAdditionalFunctions(Map.entry("SIGMA", new SigmaFunction()));
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
            if (start > end) return EvaluationValue.numberValue(BigDecimal.ZERO);
            int count = end - start + 1;
            if (count > SIGMA_MAX_ITERATIONS) {
                throw new EvaluationException(functionToken,
                        "SIGMA: too many iterations (" + count + ")");
            }
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = start; i <= end; i++) {
                Expression sub = new Expression(body, config).with("i", BigDecimal.valueOf(i));
                try {
                    sum = sum.add(sub.evaluate().getNumberValue());
                } catch (com.ezylang.evalex.parser.ParseException e) {
                    throw new EvaluationException(functionToken,
                            "SIGMA: error in body expression: " + e.getMessage());
                }
            }
            return EvaluationValue.numberValue(sum);
        }
    }

    static BigDecimal calc(String expr) {
        return new Expression(expr, config).evaluate().getNumberValue()
                .setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    static void check(String name, String expr, BigDecimal expected) {
        try {
            BigDecimal result = calc(expr);
            if (result.compareTo(expected) == 0) {
                passed++;
                System.out.println("  PASS: " + name + " = " + result);
            } else {
                failed++;
                System.out.println("  FAIL: " + name);
                System.out.println("        expected: " + expected);
                System.out.println("        got     : " + result);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("  FAIL: " + name + " (threw " + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
        }
    }

    static BigDecimal d(String s) { return new BigDecimal(s); }

    public static void main(String[] args) {
        System.out.println("= EvalEx + SIGMA accuracy tests =\n");

        System.out.println("--- Basic arithmetic ---");
        check("1+1", "1+1", d("2"));
        check("3*4", "3*4", d("12"));
        check("10/3", "10/3", d("3.3333333333"));
        check("2^10", "2^10", d("1024"));
        check("(1+2)*3", "(1+2)*3", d("9"));
        check("10%3", "10%3", d("1"));
        check("-5+8", "-5+8", d("3"));
        check("2.5*4", "2.5*4", d("10"));
        check("implicit 2(3+4)", "2(3+4)", d("14"));

        System.out.println("\n--- Built-in functions ---");
        check("SQRT(16)", "SQRT(16)", d("4"));
        check("SQRT(2)", "SQRT(2)", d("1.4142135624"));
        check("ABS(-5)", "ABS(-5)", d("5"));
        check("ROUND(3.14159, 2)", "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR(3.9)", "FLOOR(3.9)", d("3"));
        check("CEILING(3.1)", "CEILING(3.1)", d("4"));
        check("LOG(~e)", "LOG(2.718281828)", d("0.9999999993"));
        check("LOG10(100)", "LOG10(100)", d("2"));
        check("SIN(0)", "SIN(0)", d("0"));
        check("COS(0)", "COS(0)", d("1"));
        check("TAN(0)", "TAN(0)", d("0"));
        check("EXP(1)", "EXP(1)", d("2.7182818285"));
        check("FACT(5)", "FACT(5)", d("120"));
        check("MIN", "MIN(3, 1, 4, 2)", d("1"));
        check("MAX", "MAX(3, 1, 4, 2)", d("4"));

        System.out.println("\n--- Boolean ---");
        check("IF 1>0", "IF(1>0, 100, 0)", d("100"));
        check("IF 1==1", "IF(1==1, 200, 0)", d("200"));
        check("IF 1!=2", "IF(1!=2, 300, 0)", d("300"));
        check("IF AND", "IF(1<2 AND 2<3, 400, 0)", d("400"));
        check("IF OR",  "IF(1>2 OR 2<3, 500, 0)", d("500"));
        check("IF NOT", "IF(NOT(1>2), 600, 0)", d("600"));

        System.out.println("\n--- SIGMA ---");
        check("SIGMA(1,10,\"i\")      sum i",     "SIGMA(1, 10, \"i\")", d("55"));
        check("SIGMA(1,10,\"i*i\")    sum i^2",   "SIGMA(1, 10, \"i*i\")", d("385"));
        check("SIGMA(1,5,\"i^3\")     sum i^3",   "SIGMA(1, 5, \"i^3\")", d("225"));
        check("SIGMA(1,100,\"1\")     counting",  "SIGMA(1, 100, \"1\")", d("100"));
        check("SIGMA(1,4,\"FACT(i)\") sum i!",    "SIGMA(1, 4, \"FACT(i)\")", d("33"));
        check("SIGMA(1,1,\"42\")      single",    "SIGMA(1, 1, \"42\")", d("42"));
        check("SIGMA(0,0,\"99\")      zero index","SIGMA(0, 0, \"99\")", d("99"));
        check("SIGMA(5,1,\"i\")       start>end", "SIGMA(5, 1, \"i\")", d("0"));

        System.out.println("\n--- SIGMA advanced ---");
        check("SIGMA + EXP",  "SIGMA(1, 5, \"EXP(-0.1*i)\")",
                d("4.4959131450"));
        check("SIGMA + SQRT", "SIGMA(1, 9, \"SQRT(i)\")",
                d("19.3060005260"));
        check("SIGMA + IF",   "SIGMA(1, 10, \"IF(i%2==0, i, 0)\")",
                d("30"));
        check("SIGMA large",  "SIGMA(1, 1000, \"1\")",
                d("1000"));

        System.out.println("\n--- Article formulas ---");
        check("e^{-lambda*n}", "EXP(-0.1*10)", d("0.3678794412"));
        check("ROUND(pi,2)",   "ROUND(3.14159, 2)", d("3.14"));
        check("FLOOR",         "FLOOR(3.9)", d("3"));
        check("1-e^{-ln}",     "1-EXP(-0.1*5)", d("0.3934693403"));
        check("(t-u)^6/(2s^2)","(10-5)^6/(2*2^2)", d("1953.125"));

        System.out.println("\n" + "=".repeat(40));
        System.out.println("Total: " + (passed + failed) + ", PASS: " + passed + ", FAIL: " + failed);
        if (failed > 0) {
            System.out.println("SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("ALL PASSED!");
        }
    }
}
