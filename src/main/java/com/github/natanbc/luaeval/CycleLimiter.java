package com.github.natanbc.luaeval;

import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.DebugLib;

public class CycleLimiter extends DebugLib {
    private final int maxInstructions;
    private int instructions = 0;

    CycleLimiter(int max) {
        this.maxInstructions = max;
    }

    @Override
    public void onInstruction(int pc, Varargs v, int top) {
        if(++instructions > maxInstructions) throw new CycleLimitExceededException(maxInstructions);
        super.onInstruction(pc, v, top);
    }
}
