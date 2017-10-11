package com.github.natanbc.luaeval.utils;

import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.Objects;

public class LuaObject extends LuaTable {
    final Object instance;

    LuaObject(Object instance) {
        this.instance = instance;
    }

    public Object getJavaObject() {
        return instance;
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
