package com.faboit.cheeseutils;

import com.faboit.cheeseutils.feature.pay.FormulaEngine;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormulaEngineTest {
    @Test
    void evaluatesFormulaWithPlaceholders() {
        FormulaEngine engine = new FormulaEngine();
        double value = engine.evaluate("1000*{daysplayed}+10000", Map.of("daysplayed", 5.0));
        assertEquals(15000.0, value, 0.001);
    }

    @Test
    void supportsArithmeticPrecedence() {
        FormulaEngine engine = new FormulaEngine();
        double value = engine.evaluate("2+3*4", Map.of());
        assertEquals(14.0, value, 0.001);
    }
}
