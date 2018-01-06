package com.github.natanbc.luaeval;

import com.github.natanbc.luaeval.utils.LuaObject;

@FunctionalInterface
public interface ConversionHook {
    void onObjectWrapped(LuaObject object, Object originalObject);
}
