package com.github.natanbc.luaeval;

import com.github.natanbc.luaeval.utils.EvaluatorGlobals;
import com.github.natanbc.luaeval.utils.JavaLib;
import com.github.natanbc.luaeval.utils.LuaHelper;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseIoLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseOsLib;
import org.luaj.vm2.lib.jse.LuajavaLib;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings({"Duplicates", "unused", "WeakerAccess"})
public class LuaEvaluator {
    private final List<AccessibleObject> blocked = new LinkedList<>();
    private final ClassLoader classLoader;
    private final EvaluatorGlobals globals;

    public LuaEvaluator(ClassLoader loader, int cycleLimit) {
        this.classLoader = loader;
        EvaluatorGlobals globals = new EvaluatorGlobals(this);
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new JseMathLib());
        globals.load(new JseIoLib());
        globals.load(new JseOsLib());
        globals.load(new LuajavaLib());
        globals.load(new JavaLib());
        LoadState.install(globals);
        LuaC.install(globals);
        if(cycleLimit > 0) globals.load(new CycleLimiter(cycleLimit));
        globals.set("debug", LuaValue.NIL);
        this.globals = globals;
    }

    public LuaEvaluator(int cycleLimit) {
        this(ClassLoader.getSystemClassLoader(), cycleLimit);
    }

    public LuaEvaluator() {
        this(-1);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Globals getGlobals() {
        return globals;
    }

    public boolean shouldBlockMethod(Method m) {
        return blocked.contains(m);
    }

    public boolean shouldBlockConstructor(Constructor<?> c) {
        return blocked.contains(c);
    }

    public LuaEvaluator blockMethod(Method m) {
        blocked.add(m);
        return this;
    }

    public LuaEvaluator blockConstructor(Constructor<?> c) {
        blocked.add(c);
        return this;
    }

    public LuaEvaluator addConversionHook(ConversionHook hook) {
        globals.addConversionHook(hook);
        return this;
    }

    public LuaValue eval(String code) {
        return globals.load(code).call();
    }

    public LuaEvaluator set(String key, Object value) {
        if(value == null) return remove(key);
        if(value instanceof LuaValue) {
            globals.set(key, (LuaValue)value);
        } else if(value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Character ||
                value instanceof Integer || value instanceof Float || value instanceof Long || value instanceof Double ||
                value instanceof String) {
            globals.set(key, CoerceJavaToLua.coerce(value));
        } else {
            globals.set(key, LuaHelper.coerce(this, value));
        }
        return this;
    }

    public LuaEvaluator set(int key, Object value) {
        if(value == null) return remove(key);
        if(value instanceof LuaValue) {
            globals.set(key, (LuaValue)value);
        } else if(value instanceof Boolean || value instanceof Byte || value instanceof Short || value instanceof Character ||
                value instanceof Integer || value instanceof Float || value instanceof Long || value instanceof Double ||
                value instanceof String) {
            globals.set(key, CoerceJavaToLua.coerce(value));
        } else {
            globals.set(key, LuaHelper.coerce(this, value));
        }
        return this;
    }

    public LuaEvaluator remove(String key) {
        globals.set(key, LuaValue.NIL);
        return this;
    }

    public LuaEvaluator remove(int key) {
        globals.set(key, LuaValue.NIL);
        return this;
    }

    public LuaEvaluator setObject(String key, Object value) {
        if(value == null) return remove(key);
        globals.set(key, LuaHelper.coerce(this, value));
        return this;
    }

    public LuaEvaluator setObject(int key, Object value) {
        if(value == null) return remove(key);
        globals.set(key, LuaHelper.coerce(this, value));
        return this;
    }

    public LuaEvaluator removePackage() {
        return remove("package");
    }

    public LuaEvaluator removeIO() {
        return remove("io");
    }

    public LuaEvaluator removeOS() {
        return remove("os");
    }

    public LuaEvaluator removeCoroutine() {
        return remove("coroutine");
    }

    public LuaEvaluator removeLuajava() {
        return remove("luajava");
    }
}
