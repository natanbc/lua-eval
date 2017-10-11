package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import com.github.natanbc.luaeval.LuaIgnore;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LuaHelper {
    static LuaValue toLua(LuaEvaluator evaluator, Object obj) {
        LuaValue v = CoerceJavaToLua.coerce(obj);
        if(v.isuserdata()) return coerce(evaluator, obj);
        return v;
    }

    static Object getInstance(LuaValue v, Class<?> type) {
        if(v instanceof LuaObject) {
            return ((LuaObject) v).instance;
        }
        return CoerceLuaToJava.coerce(v, type);
    }

    public static LuaTable coerce(LuaEvaluator evaluator, Object obj) {
        return coerce(evaluator, obj.getClass(), obj);
    }

    static LuaTable coerce(LuaEvaluator evaluator, Class<?> cls, Object obj) {
        LuaObject object = new LuaObject(obj);
        if(obj != null && cls.isArray()) {
            int length = Array.getLength(obj);
            for(int i = 0; i < length; i++) {
                object.set(i + 1, toLua(evaluator, Array.get(obj, i)));
            }
            object.set("clone", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    Class<?> type = obj.getClass().getComponentType();
                    Object newArray = Array.newInstance(type, length);
                    for(int i = 0; i < length; i++) {
                        Array.set(newArray, i, Array.get(obj, i));
                    }
                    return coerce(evaluator, type, newArray);
                }
            });
        }
        Map<String, List<Method>> map = new HashMap<>();
        for(Method m : cls.getMethods()) {
            if(m.getAnnotation(LuaIgnore.class) != null || evaluator.shouldBlockMethod(m)) continue;
            if(obj == null && !Modifier.isStatic(m.getModifiers())) continue;
            m.setAccessible(true);
            map.computeIfAbsent(m.getName(), ignored->new LinkedList<>()).add(m);
        }
        if(obj instanceof Class) {
            object.set("new", new New(evaluator, (Class)obj));
            for(Method m : Class.class.getMethods()) {
                m.setAccessible(true);
                map.computeIfAbsent(m.getName(), ignored->new LinkedList<>()).add(m);
            }
        }

        map.forEach((name, methods)->object.set(name, coerce(evaluator, object, obj, methods)));

        return object;
    }

    @SuppressWarnings("unchecked")
    private static LuaFunction coerce(LuaEvaluator evaluator, LuaObject object, Object instance, List<Method> methods) {
        Map<Method, Class[]> map = new HashMap<>();
        for(Method m : methods) {
            map.put(m, m.getParameterTypes());
        }

        return new VarArgFunction() {
            @Override
            public Varargs onInvoke(Varargs varargs) {
                LuaValue[] array = new LuaValue[varargs.narg()];
                for(int j = 0; j < array.length; j++) {
                    array[j] = varargs.arg(j+1);
                }
                Object[] args = null;
                Method method = null;
                for(Map.Entry<Method, Class[]> entry : map.entrySet()) {
                    if(entry.getValue().length != array.length) continue;
                    try {
                        args = args(evaluator, entry.getKey().isVarArgs(), entry.getValue(), array);
                    } catch(LuaError e) {
                        if(map.size() != 1) continue;
                        throw e;
                    }
                    method = entry.getKey();
                    break;
                }
                if(method == null) {
                    for(Map.Entry<Method, Class[]> entry : map.entrySet()) {
                        try {
                            args = args(evaluator, entry.getKey().isVarArgs(), entry.getValue(), array);
                        } catch(LuaError e) {
                            continue;
                        }
                        method = entry.getKey();
                        break;
                    }
                }
                if(method == null) {
                    throw new LuaError("No matching method found");
                }
                try {
                    Object o = method.invoke(instance, args);
                    if(o == instance && object != null) return object;
                    if(o == null) return NIL;
                    LuaValue v = CoerceJavaToLua.coerce(o);
                    if(v.isuserdata()) {
                        return coerce(evaluator, o);
                    }
                    return v;
                } catch(InvocationTargetException e) {
                    throw new LuaError(e.getCause());
                } catch(Exception e) {
                    throw new LuaError(e);
                }
            }
        };
    }

    static Object[] args(LuaEvaluator evaluator, boolean isVarargs, Class<?>[] argTypes, LuaValue[] v) {
        Object[] a = new Object[argTypes.length];
        for(int i = 0; i < argTypes.length; i++) {
            Class<?> cls = argTypes[i];
            if(i >= v.length) {
                if(isVarargs && i == argTypes.length-1) {
                    a[i] = Array.newInstance(cls.getComponentType(), 0);
                }
                else if(cls == boolean.class) a[i] = false;
                else if(cls == byte.class) a[i] = (byte)0;
                else if(cls == short.class) a[i] = (short)0;
                else if(cls == char.class) a[i] = (char)0;
                else if(cls == int.class) a[i] = 0;
                else if(cls == float.class) a[i] = (float)0;
                else if(cls == long.class) a[i] = (long)0;
                else if(cls == double.class) a[i] = (double)0;
            } else {
                LuaValue value = v[i];
                if(cls == boolean.class) {
                    if(!value.isboolean()) throw new LuaError("TypeError: boolean expected, got " + value.typename());
                    a[i] = value.toboolean();
                } else if(cls == byte.class || cls == short.class || cls == int.class || cls == float.class || cls == long.class || cls == double.class) {
                    if(!value.isnumber()) throw new LuaError("TypeError: number expected, got " + value.typename());
                    if(cls == byte.class) a[i] = value.tobyte();
                    else if(cls == short.class) a[i] = value.toshort();
                    else if(cls == int.class) a[i] = value.toint();
                    else if(cls == float.class) a[i] = value.tofloat();
                    else if(cls == long.class) a[i] = value.tolong();
                    else if(cls == double.class) a[i] = value.todouble();
                } else if(cls == char.class) {
                    if(!value.isstring() || value.tojstring().length() != 1)  throw new LuaError("TypeError: single char string expected, got " + value.typename());
                    a[i] = value.tojstring().charAt(0);
                } else if(cls == Boolean.class) {
                    if(!value.isnil() && !value.isboolean()) throw new LuaError("TypeError: boolean or nil expected, got " + value.typename());
                    a[i] = value.isnil() ? null : value.toboolean();
                } else if(cls == Byte.class || cls == Short.class || cls == Integer.class || cls == Float.class || cls == Long.class || cls == Double.class) {
                    if(!value.isnil() && !value.isnumber()) throw new LuaError("TypeError: number or nil expected, got " + value.typename());
                    if(cls == Byte.class) a[i] = value.isnil() ? null : value.tobyte();
                    else if(cls == Short.class) a[i] = value.isnil() ? null : value.toshort();
                    else if(cls == Integer.class) a[i] = value.isnil() ? null : value.toint();
                    else if(cls == Float.class) a[i] = value.isnil() ? null : value.tofloat();
                    else if(cls == Long.class) a[i] = value.isnil() ? null : value.tolong();
                    else if(cls == Double.class) a[i] = value.isnil() ? null : value.todouble();
                } else if(cls == Character.class) {
                    if(!value.isnil() && (!value.isstring() || value.tojstring().length() != 1)) throw new LuaError("TypeError: single char string or nil expected, got " + value.typename());
                    a[i] = value.isnil() ? null : value.tojstring().charAt(0);
                } else if(cls == String.class) {
                    if(!value.isnil() && !value.isstring()) throw new LuaError("TypeError: string or nil expected, got " + value.typename());
                    a[i] = value.isnil() ? null : value.tojstring();
                } else {
                    if(cls == CharSequence.class) {
                        if(value.isnil()) {
                            a[i] = null;
                            continue;
                        } else if(value.isstring()) {
                            a[i] = value.tojstring();
                            continue;
                        }
                    }
                    if(value instanceof LuaObject) {
                        Object instance = ((LuaObject) value).instance;
                        if(cls.isInstance(instance)) {
                            a[i] = instance;
                        } else {
                            throw new LuaError("TypeError: " + cls.getName() + " expected, got " + value.typename());
                        }
                    } else if(cls == Object.class) {
                        a[i] = CoerceLuaToJava.coerce(value, Object.class);
                    } else if(cls.isArray() && value.istable()) {
                        Class<?> type = cls.getComponentType();
                        Class<?>[] c = new Class[value.length()];
                        Arrays.fill(c, type);
                        Varargs varargs = ((LuaTable)value).unpack();
                        LuaValue[] array = new LuaValue[varargs.narg()];
                        for(int j = 0; j < array.length; j++) {
                            array[j] = varargs.arg(j+1);
                        }
                        Object[] o = args(evaluator, false, c, array);
                        Object jArray = Array.newInstance(type, array.length);
                        for(int j = 0; j < o.length; j++) {
                            Array.set(jArray, j, o[j]);
                        }
                        a[i] = jArray;
                    } else if(cls.isArray() && (isVarargs && i == argTypes.length -1)) {
                        Class<?> type = cls.getComponentType();
                        Class<?>[] c = new Class[v.length-i];
                        Arrays.fill(c, type);
                        LuaValue[] array = Arrays.copyOfRange(v, i, v.length);
                        Object[] o = args(evaluator, false, c, array);
                        Object jArray = Array.newInstance(type, o.length);
                        for(int j = 0; j < o.length; j++) {
                            Array.set(jArray, j, o[j]);
                        }
                        a[i] = jArray;
                    } else if(LuaLambda.isLambda(cls) && value.isfunction()) {
                        a[i] = LuaLambda.toLambda(evaluator, cls, (LuaFunction)value);
                    } else if(!value.isnil()) {
                        throw new LuaError("TypeError: " + cls.getName() + " expected, got " + value.typename());
                    } else {
                        a[i] = null;
                    }
                }
            }
        }
        return a;
    }

    private static class New extends VarArgFunction {
        private final LuaEvaluator evaluator;
        private final Constructor<?>[] constructors;

        New(LuaEvaluator evaluator, Class<?> cls) {
            this.evaluator = evaluator;
            this.constructors = cls.getConstructors();
            AccessibleObject.setAccessible(constructors, true);
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaValue[] v = new LuaValue[args.narg()];
            for(int i = 0; i < v.length; i++) {
                v[i] = args.arg(i+1);
            }
            Constructor<?> constructor = null;
            Object[] a = null;
            for(Constructor<?> ctor : constructors) {
                if(evaluator.shouldBlockConstructor(ctor)) continue;
                Class<?>[] argTypes = ctor.getParameterTypes();
                if(argTypes.length != v.length) continue;
                try {
                    a = args(evaluator, ctor.isVarArgs(), argTypes, v);
                } catch(LuaError e) {
                    if(constructors.length != 1) continue;
                    throw e;
                }
                constructor = ctor;
                break;
            }

            if(constructor == null) {
                for(Constructor<?> ctor : constructors) {
                    if(evaluator.shouldBlockConstructor(ctor)) continue;
                    Class<?>[] argTypes = ctor.getParameterTypes();
                    a = args(evaluator, ctor.isVarArgs(), argTypes, v);
                    constructor = ctor;
                    break;
                }
            }

            if(constructor == null) {
                throw new LuaError("No matching constructor found");
            }

            try {
                return varargsOf(new LuaValue[] {LuaHelper.coerce(evaluator, constructor.newInstance(a))});
            } catch(IllegalAccessException e) {
                throw new AssertionError();
            } catch(InvocationTargetException e) {
                LuaError error = new LuaError(e.getCause().toString());
                error.setStackTrace(e.getCause().getStackTrace());
                throw error;
            } catch(InstantiationException e) {
                LuaError error = new LuaError(e.toString());
                error.setStackTrace(e.getStackTrace());
                throw error;
            }
        }
    }
}
