package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class LuaInterface {
    @SuppressWarnings("unchecked")
    static <T> T implement(LuaEvaluator evaluator, Class<T> clazz, LuaTable methodTable, Method... methods) {
        Map<MethodReference, LuaFunction> map = new HashMap<>();
        for(Method m : methods) {
            LuaValue v = methodTable.get(m.getName());
            if(!v.isfunction()) {
                if(m.isDefault()) {
                    continue;
                } else {
                    LuaValue.error("No implementation defined for method " + m.getName());
                }
            }
            map.put(new MethodReference(m), (LuaFunction)v);
        }

        return (T) Proxy.newProxyInstance(evaluator.getClassLoader(), new Class[]{clazz}, new Handler(evaluator, map));
    }

    static <T> T implement(LuaEvaluator evaluator, Class<T> clazz, LuaTable methodTable) {
        return implement(evaluator, clazz, methodTable, getUnimplementedMethods(clazz));
    }

    private static Method[] getUnimplementedMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods()).filter(m->!Modifier.isStatic(m.getModifiers())).toArray(Method[]::new);
    }

    public static class Handler implements InvocationHandler {
        private final LuaEvaluator evaluator;
        private final Map<MethodReference, LuaFunction> functions;

        Handler(LuaEvaluator evaluator, Map<MethodReference, LuaFunction> functions) {
            this.evaluator = evaluator;
            this.functions = functions;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            MethodReference r = new MethodReference(method);
            LuaFunction f = functions.get(r);
            if(f == null) return method.invoke(proxy, args);
            if(args == null) args = new Object[0];
            LuaValue[] luaArgs = new LuaValue[args.length];
            for(int i = 0; i < luaArgs.length; i++) {
                luaArgs[i] = LuaHelper.toLua(evaluator, args[i]);
            }
            LuaValue obj = f.invoke(luaArgs).arg1();
            Class<?> retType = method.getReturnType();
            if(retType == void.class) return null;
            switch(retType.getName()) {
                case "boolean":
                case "java.lang.Boolean":
                    return obj.isboolean() ? obj.toboolean() : !obj.isnil();
                case "char":
                case "java.lang.Character":
                    return obj.tochar();
                case "byte":
                case "java.lang.Byte":
                    return obj.checknumber().tobyte();
                case "short":
                case "java.lang.Short":
                    return obj.checknumber().toshort();
                case "int":
                case "java.lang.Integer":
                    return obj.checknumber().toint();
                case "float":
                case "java.lang.Float":
                    return obj.checknumber().tofloat();
                case "long":
                case "java.lang.Long":
                    return obj.checknumber().tolong();
                case "double":
                case "java.lang.Double":
                    return obj.checknumber().todouble();
            }
            return LuaHelper.getInstance(obj, retType);
        }
    }

    public static class MethodReference {
        private final String name;
        private final Class<?>[] params;

        MethodReference(Method method) {
            this.name = method.getName();
            this.params = method.getParameterTypes();
        }

        @Override
        public int hashCode() {
            return name.hashCode() ^ Arrays.hashCode(params);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof MethodReference) {
                MethodReference r = (MethodReference)obj;
                return name.equals(r.name) && Arrays.equals(params, r.params);
            }
            return false;
        }
    }
}
