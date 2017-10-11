package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class JavaLib extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue arg1, LuaValue env) {
        if(!(env instanceof EvaluatorGlobals)) throw new UnsupportedOperationException("env not an instance of EvaluatorGlobals");
        LuaEvaluator evaluator = ((EvaluatorGlobals) env).evaluator;
        LuaTable java = new LuaTable();
        java.set("type", new Type(evaluator));
        java.set("implement", new Implement(evaluator));

        env.set("Java", java);

        return env;
    }

    private static class Type extends OneArgFunction {
        private final LuaEvaluator evaluator;

        Type(LuaEvaluator evaluator) {
            this.evaluator = evaluator;
        }


        @Override
        public LuaValue call(LuaValue arg) {
            try {
                Class<?> cls = Class.forName(arg.checkjstring(), false, evaluator.getClassLoader());
                return LuaHelper.coerce(evaluator, cls, cls);
            } catch(ClassNotFoundException e) {
                LuaValue.error("Class not found");
                throw new AssertionError();
            }
        }
    }

    private static class Implement extends TwoArgFunction {
        private final LuaEvaluator evaluator;

        Implement(LuaEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public LuaValue call(LuaValue arg1, LuaValue arg2) {
            if(arg1.isstring()) {
                try {
                    return LuaHelper.coerce(evaluator, LuaInterface.implement(evaluator, Class.forName(arg1.checkjstring(), false, evaluator.getClassLoader()), arg2.checktable()));
                } catch(ClassNotFoundException e) {
                    throw new LuaError("Class not found");
                }
            } else if(arg1 instanceof LuaObject) {
                Object instance = ((LuaObject) arg1).instance;
                if(instance instanceof Class<?>) {
                    return LuaHelper.coerce(evaluator, LuaInterface.implement(evaluator, (Class<?>)instance, arg2.checktable()));
                } else {
                    throw new LuaError("TypeError: class or string expected, got " + arg1.typename());
                }
            } else {
                throw new LuaError("TypeError: class or string expected, got " + arg1.typename());
            }
        }
    }
}
