package com.github.natanbc.luaeval.scriptengine;

import com.github.natanbc.luaeval.LuaEvaluator;
import com.github.natanbc.luaeval.utils.LuaObject;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.IOException;
import java.io.Reader;

public class Engine extends AbstractScriptEngine {
    @SuppressWarnings("WeakerAccess")
    public static final String ATTRIBUTE_MAX_CYCLES = "Lua.MAX_CYCLES";

    private final Factory factory;

    Engine(Factory factory) {
        super(new SimpleBindings());
        this.factory = factory;
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        Object o = context.getAttribute(ATTRIBUTE_MAX_CYCLES);
        Bindings b = context.getBindings(ScriptContext.ENGINE_SCOPE);
        LuaEvaluator evaluator = new LuaEvaluator(o instanceof Number ? ((Number)o).intValue() : -1);
        b.forEach(evaluator::set);
        LuaValue v = evaluator.eval(script);
        if(v instanceof LuaObject) {
            return ((LuaObject) v).getJavaObject();
        }
        return CoerceLuaToJava.coerce(v, Object.class);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        try {
            char[] arr = new char[1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            reader.close();
            return eval(buffer.toString(), context);
        } catch(IOException e) {
            throw new ScriptException(e);
        }
    }

    @Override
    public Bindings createBindings() {
        Bindings b = getBindings(ScriptContext.ENGINE_SCOPE);
        if(b == null) setBindings(b = new SimpleBindings(), ScriptContext.ENGINE_SCOPE);
        return b;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }
}
