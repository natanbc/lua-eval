package com.github.natanbc.luaeval;

@SuppressWarnings("unused")
public class CycleLimitExceededException extends RuntimeException {
    private final int maxCycles;

    CycleLimitExceededException(int max) {
        this.maxCycles = max;
    }

    public int getMaxCycles() {
        return maxCycles;
    }
}
