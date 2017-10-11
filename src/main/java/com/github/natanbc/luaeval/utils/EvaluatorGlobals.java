package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.Globals;

public class EvaluatorGlobals extends Globals {
    final LuaEvaluator evaluator;

    public EvaluatorGlobals(LuaEvaluator evaluator) {
        this.evaluator = evaluator;
    }
}
