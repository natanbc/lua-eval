package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

class LuaLambda {
    private static final Method[] OBJECT_METHODS = Object.class.getDeclaredMethods();

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
        outer: for(Method method : lambdaClass.getMethods()) {
            if(method.isDefault() || Modifier.isStatic(method.getModifiers())) continue;
            /*
            The following check is to detect cases of interfaces declaring methods with the same signature as
            methods in Object, which fail the above check but aren't the wanted method (eg on Comparator):

            @FunctionalInterface
            interface MyInterface {
                void myMethod(); //wanted method

                boolean equals(Object other); //would pass the above check, but isn't the method we want
            }
            */
            for(Method m : OBJECT_METHODS) {
                if(m.getName().equals(method.getName()) && Arrays.equals(m.getParameterTypes(), method.getParameterTypes())) continue outer;
            }
            return method;
        }
        throw new AssertionError();
    }
}
