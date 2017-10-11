package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

class LuaLambda {
    @SuppressWarnings("unchecked")
    static <T> T toLambda(LuaEvaluator evaluator, Class<T> lambdaClass, LuaFunction function) {
        if(!lambdaClass.isInterface() || lambdaClass.getAnnotation(FunctionalInterface.class) == null) throw new UnsupportedOperationException(lambdaClass + " is not a FunctionalInterface");
        Method lambdaMethod = findLambdaMethod(lambdaClass);

        LuaTable t = new LuaTable();
        t.set(lambdaMethod.getName(), function);
        return LuaInterface.implement(evaluator, lambdaClass, t, lambdaMethod);
    }

    static boolean isLambda(Class<?> clazz) {
        return clazz.isInterface() && clazz.getAnnotation(FunctionalInterface.class) != null;
    }

    private static Method findLambdaMethod(Class<?> lambdaClass) {
        for(Method method : lambdaClass.getMethods()) {
            if(method.isDefault() || Modifier.isStatic(method.getModifiers())) continue;
            return method;
        }
        throw new AssertionError();
    }
}
