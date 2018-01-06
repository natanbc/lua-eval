package com.github.natanbc.luaeval.utils;

import com.github.natanbc.luaeval.LuaEvaluator;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNumber;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Objects;

public class LuaObject extends LuaTable {
    private final LuaEvaluator evaluator;
    final Object instance;
    private final Class<?> arrayClass;
    private final Field[] fields;

    LuaObject(LuaEvaluator evaluator, Object instance, Field[] fields) {
        this.evaluator = evaluator;
        this.instance = instance;
        this.arrayClass = instance != null && instance.getClass().isArray() ? instance.getClass() : null;
        this.fields = fields;
        AccessibleObject.setAccessible(fields, true);
        ((EvaluatorGlobals)evaluator.getGlobals()).hooks.forEach(hook->hook.onObjectWrapped(this, instance));
    }

    public Object getJavaObject() {
        return instance;
    }

    @Override
    public LuaValue setmetatable(LuaValue metatable) {
        return error("Objects cannot have metatables");
    }

    @Override
    public LuaValue get(LuaValue key) {
        LuaValue v = super.rawget(key);
        if(v.isnil() && key.isstring() && !key.isnumber()) {
            String s = key.tojstring();
            if(arrayClass != null && s.equals("length")) {
                return LuaNumber.valueOf(Array.getLength(instance));
            }
            for(Field f : fields) {
                if(f.getName().equals(s)) {
                    try {
                        Object obj = f.get(instance);
                        if(obj == null) return v;
                        if(obj instanceof LuaValue) return (LuaValue)obj;
                        return LuaHelper.coerce(evaluator, obj);
                    } catch(IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        } else if(v.isnil() && key.isnumber()) {
            if(arrayClass == null) return v;
            double d = key.todouble();
            if(d % 1 != 0 || d > Integer.MAX_VALUE || d < 1) return v;
            int i = (int)d;
            int len = Array.getLength(instance);
            if(i > len) {
                return v;
            }
            i--;
            if(arrayClass == boolean[].class) {
                return LuaBoolean.valueOf(((boolean[])instance)[i]);
            } else if(arrayClass == byte[].class) {
                return LuaNumber.valueOf(((byte[])instance)[i]);
            } else if(arrayClass == short[].class) {
                return LuaNumber.valueOf(((short[])instance)[i]);
            } else if(arrayClass == char[].class) {
                return LuaString.valueOf(new String((char[])instance, i, 1));
            } else if(arrayClass == int[].class) {
                return LuaNumber.valueOf(((int[])instance)[i]);
            } else if(arrayClass == float[].class) {
                return LuaNumber.valueOf(((float[])instance)[i]);
            } else if(arrayClass == long[].class) {
                return LuaNumber.valueOf(((long[])instance)[i]);
            } else if(arrayClass == double[].class) {
                return LuaNumber.valueOf(((double[])instance)[i]);
            } else {
                return LuaHelper.coerce(evaluator, arrayClass.getComponentType(), ((Object[])instance)[i]);
            }
        }
        return v;
    }

    @Override
    public void set(LuaValue key, LuaValue value) {
        if(key.isstring() && !key.isnumber()) {
            String s = key.tojstring();
            if(arrayClass != null && s.equals("length")) {
                throw new LuaError("Java array length cannot be modified");
            }
            for(Field f : fields) {
                if(f.getName().equals(s)) {
                    try {
                        Object obj = LuaHelper.getInstance(value, Object.class);
                        if(!f.getType().isInstance(obj) && obj != null) {
                            throw new LuaError("TypeError: " + f.getType().getName() + " expected, got " + value.typename());
                        }
                        f.set(instance, obj);
                    } catch(IllegalAccessException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        } else if(key.isnumber()) {
            if(arrayClass == null) {
                super.set(key, value);
                return;
            }
            double d = key.todouble();
            if(d % 1 != 0 || d > Integer.MAX_VALUE || d < 1) {
                super.set(key, value);
                return;
            }
            int i = (int)d;
            int len = Array.getLength(instance);
            if(i > len) {
                super.set(key, value);
                return;
            }
            i--;
            if(arrayClass == boolean[].class) {
                ((boolean[])instance)[i] = value.checkboolean();
            } else if(arrayClass == byte[].class) {
                ((byte[])instance)[i] = value.checknumber().tobyte();
            } else if(arrayClass == short[].class) {
                ((short[])instance)[i] = value.checknumber().toshort();
            } else if(arrayClass == char[].class) {
                if(value.checkstring().m_length != 1) throw new LuaError("TypeError: expected a single char, got multiple");
                ((char[])instance)[i] = value.tojstring().charAt(0);
            } else if(arrayClass == int[].class) {
                ((int[])instance)[i] = value.checknumber().toint();
            } else if(arrayClass == float[].class) {
                ((float[])instance)[i] = value.checknumber().tofloat();
            } else if(arrayClass == long[].class) {
                ((long[])instance)[i] = value.checknumber().tolong();
            } else if(arrayClass == double[].class) {
                ((double[])instance)[i] = value.checknumber().todouble();
            } else {
                Object obj = LuaHelper.getInstance(value, Object.class);
                if(!arrayClass.getComponentType().isInstance(obj) && obj != null) {
                    throw new LuaError("TypeError: " + arrayClass.getComponentType().getName() + " expected, got " + value.typename());
                }
                ((Object[])instance)[i] = obj;
            }
            return;
        }
        super.set(key, value);
    }

    @Override
    public String typename() {
        return instance == null ? "null" : instance.getClass().getName();
    }

    @Override
    public LuaValue eq(LuaValue val) {
        return LuaBoolean.valueOf(equals(val));
    }

    @Override
    public LuaValue tostring() {
        return LuaString.valueOf(toString());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(instance);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LuaObject && Objects.equals(instance, ((LuaObject) obj).instance);
    }

    @Override
    public String toString() {
        return String.valueOf(instance);
    }
}
