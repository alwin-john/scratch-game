package com.betting.scratchgame.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Probabilities {
    @JsonProperty("standard_symbols")
    private List<CellProbability> standardSymbols;

    @JsonProperty("bonus_symbols")
    private BonusProbability bonusSymbols;

    public List<CellProbability> getStandardSymbols() {
        return standardSymbols;
    }

    public void setStandardSymbols(List<CellProbability> standardSymbols) {
        this.standardSymbols = standardSymbols;
    }

    public BonusProbability getBonusSymbols() {
        return bonusSymbols;
    }

    public void setBonusSymbols(BonusProbability bonusSymbols) {
        this.bonusSymbols = bonusSymbols;
    }
}
