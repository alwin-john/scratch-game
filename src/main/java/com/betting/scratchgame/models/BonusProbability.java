package com.betting.scratchgame.models;

import java.util.Map;

public class BonusProbability {
    private Map<String, Integer> symbols;

    public Map<String, Integer> getSymbols() {
        return symbols;
    }

    public void setSymbols(Map<String, Integer> symbols) {
        this.symbols = symbols;
    }
}
