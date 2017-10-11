package com.github.natanbc.luaeval.scriptengine;

import org.luaj.vm2.Lua;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Factory implements ScriptEngineFactory {
    @Override
    public String getEngineName() {
        return "luaeval";
    }

    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("lua");
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.singletonList("text/*");
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList("lua", "luaeval");
    }

    @Override
    public String getLanguageName() {
        return "Lua (" + Lua._VERSION + ")";
    }

    @Override
    public String getLanguageVersion() {
        return Lua._VERSION;
    }

    @Override
    public Object getParameter(String key) {
        switch(key) {
            case "ScriptEngine.ENGINE": return getEngineName();
            case "ScriptEngine.ENGINE_VERSION": return getEngineVersion();
            case "ScriptEngine.LANGUAGE": return getLanguageName();
            case "ScriptEngine.LANGUAGE_VERSION": return getLanguageVersion();
            case "ScriptEngine.NAME": return "luaeval";
            case "THREADING": return null; //we are not thread safe
        }
        return null;
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder sb = new StringBuilder(obj);
        sb.append(".").append(m).append("(");
        for(int i = 0; i < args.length; i++) {
            if(i != 0) sb.append(", ");
            sb.append(args[i]);
        }
        return sb.append(")").toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        return String.join(" ", statements);
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new Engine(this);
    }
}
