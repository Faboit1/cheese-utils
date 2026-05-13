package com.faboit.cheeseutils.feature.pay;

import java.util.Map;

public final class FormulaEngine {
    public double evaluate(String formula, Map<String, Double> values) {
        String expr = formula;
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            expr = expr.replace("{" + entry.getKey() + "}", Double.toString(entry.getValue()));
        }
        return parse(expr.replace(" ", ""));
    }

    private double parse(String expression) {
        return new Object() {
            int pos = -1;
            int ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new IllegalArgumentException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                while (true) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                while (true) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, pos));
                } else {
                    throw new IllegalArgumentException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}
