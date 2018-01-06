package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.ConversionHook;
import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.Globals;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class EvaluatorGlobals extends Globals {
    final LuaEvaluator evaluator;
    final List<ConversionHook> hooks = new LinkedList<>();

    public EvaluatorGlobals(LuaEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public EvaluatorGlobals addConversionHook(ConversionHook hook) {
        Objects.requireNonNull(hook);
        hooks.add(hook);
        return this;
    }
}
