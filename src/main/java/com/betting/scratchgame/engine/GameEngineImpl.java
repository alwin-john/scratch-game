package com.betting.scratchgame.engine;

import com.betting.scratchgame.models.*;

import java.util.*;

public class GameEngineImpl implements GameEngine {

    private GameConfig gameConfig;
    private final int betAmount;
    private final int rows;
    private final int cols;
    private final Random random = new Random();

    public GameEngineImpl(GameConfig gameConfig, int betAmount) {
        this.gameConfig = gameConfig;
        this.betAmount = betAmount;
        this.cols = (gameConfig.getColumns() > 0) ? gameConfig.getColumns() : 3;
        this.rows = (gameConfig.getRows() > 0) ? gameConfig.getRows() : 3;
    }

    public GeneratedMatrixInfo instantiateBoard() {
        GeneratedMatrixInfo generatedMatrixInfo = new GeneratedMatrixInfo();
        String[][] matrix = new String[rows][cols];
        Map<String, CellProbability> cellProbMap = new HashMap<>();
        for (CellProbability cp : gameConfig.getProbabilities().getStandardSymbols()) {
            String key = cp.getColumn() + ":" + cp.getRow();
            int total = cp.getSymbols().values().stream().mapToInt(Integer::intValue).sum();
            for (Map.Entry<String, Integer> entry : cp.getSymbols().entrySet()) {
                int probability = (int) Math.round(entry.getValue() * 100) / total;
                entry.setValue(probability);
            }
            cellProbMap.put(key, cp);
        }
        // Use the first entry as default if a cell is missing.
        CellProbability defaultCP = gameConfig.getProbabilities().getStandardSymbols().get(0);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String key = c + ":" + r;
                CellProbability cp = cellProbMap.getOrDefault(key, defaultCP);
                matrix[r][c] = sampleFromProbability(cp.getSymbols());
            }
        }

        String bonusSymbol = sampleFromProbability(gameConfig.getProbabilities().getBonusSymbols().getSymbols());
        int bonusRow = random.nextInt(rows);
        int bonusCol = random.nextInt(cols);
        matrix[bonusRow][bonusCol] = bonusSymbol;
        generatedMatrixInfo.setMatrix(matrix);
        generatedMatrixInfo.setBonusSymbol(bonusSymbol);
        return generatedMatrixInfo;
    }

    @Override
    public GameResult play(GeneratedMatrixInfo matrixInfo) {
        String[][] matrix = matrixInfo.getMatrix();
        String bonusSymbol = matrixInfo.getBonusSymbol();
        Map<String, Integer> symbolCounts = new HashMap<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String sym = matrix[r][c];
                if (gameConfig.getSymbols().containsKey(sym) &&
                        "standard".equalsIgnoreCase(gameConfig.getSymbols().get(sym).getType())) {
                    symbolCounts.put(sym, symbolCounts.getOrDefault(sym, 0) + 1);
                }
            }
        }

        Map<String, Map<String, Double>> symbolWinMultipliers = new HashMap<>();
        for (String sym : symbolCounts.keySet()) {
            symbolWinMultipliers.put(sym, new HashMap<>());
        }
        findWinningCombinationForSameSymbols(symbolCounts, symbolWinMultipliers);
        findWinningCombinationForLinearSymbols(matrix, symbolWinMultipliers);
        return calculateREwardAndGenerateGameOutput(symbolWinMultipliers, bonusSymbol, matrix);
    }

    private void findWinningCombinationForSameSymbols(Map<String, Integer> symbolCounts, Map<String, Map<String, Double>> symbolWinMultipliers) {
        for (Map.Entry<String, WinCombination> wcEntry : gameConfig.getWinCombinations().entrySet()) {
            WinCombination wc = wcEntry.getValue();
            if ("same_symbols".equalsIgnoreCase(wc.getWhen())) {
                for (String sym : symbolCounts.keySet()) {
                    int count = symbolCounts.get(sym);
                    if (count >= wc.getCount()) {
                        // For each win-group, record only one win the one with the higher multiplier
                        double current = symbolWinMultipliers.get(sym).getOrDefault(wc.getGroup(), 0.0);
                        if (wc.getRewardMultiplier() > current) {
                            symbolWinMultipliers.get(sym).put(wc.getGroup(), wc.getRewardMultiplier());
                        }
                    }
                }
            }
        }
    }

    private void findWinningCombinationForLinearSymbols(String[][] matrix, Map<String, Map<String, Double>> symbolWinMultipliers) {
        for (Map.Entry<String, WinCombination> wcEntry : gameConfig.getWinCombinations().entrySet()) {
            WinCombination wc = wcEntry.getValue();
            if ("linear_symbols".equalsIgnoreCase(wc.getWhen())) {
                for (List<String> area : wc.getCoveredAreas()) {
                    String firstSymbol = null;
                    boolean valid = true;
                    for (String pos : area) {
                        String[] parts = pos.split(":");
                        int col = Integer.parseInt(parts[0]);
                        int row = Integer.parseInt(parts[1]);
                        // Check if within bounds.
                        if (row < 0 || row >= rows || col < 0 || col >= cols) {
                            valid = false;
                            break;
                        }
                        String cellSym = matrix[row][col];
                        if (firstSymbol == null) {
                            firstSymbol = cellSym;
                        } else {
                            if (!cellSym.equals(firstSymbol)) {
                                valid = false;
                                break;
                            }
                        }
                    }
                    if (valid && firstSymbol != null) {
                        double current = symbolWinMultipliers.get(firstSymbol).getOrDefault(wc.getGroup(), 1.0);
                        if (wc.getRewardMultiplier() > current) {
                            symbolWinMultipliers.get(firstSymbol).put(wc.getGroup(), wc.getRewardMultiplier());
                        }
                    }
                }
            }
        }
    }

    private GameResult calculateREwardAndGenerateGameOutput(Map<String, Map<String, Double>> symbolWinMultipliers, String bonusSymbol, String[][] matrix) {
        double totalReward = 0.0;
        Map<String, List<String>> appliedWinningCombinations = new HashMap<>();
        for (String sym : symbolWinMultipliers.keySet()) {
            Map<String, Double> multipliers = symbolWinMultipliers.get(sym);
            double product = 1.0;
            for (double m : multipliers.values()) {
                product *= m;
            }
            if (!multipliers.isEmpty()) {
                double symbolBase = gameConfig.getSymbols().get(sym).getRewardMultiplier();
                double rewardForSym = betAmount * symbolBase * product;
                totalReward += rewardForSym;

                // get the key of multipliers from the group id
                List<String> wins = new ArrayList<>();
                for (Map.Entry<String, WinCombination> wcEntry : gameConfig.getWinCombinations().entrySet()) {
                    WinCombination wc = wcEntry.getValue();
                    if (multipliers.containsKey(wc.getGroup()) && wc.getRewardMultiplier() == multipliers.get(wc.getGroup())) {
                        wins.add(wcEntry.getKey());
                    }
                }
                appliedWinningCombinations.put(sym, wins);
            }
        }

        // apply bonus symbol effect.
        if (totalReward > 0) {
            SymbolConfig bonusConfig = gameConfig.getSymbols().get(bonusSymbol);
            if ("multiply_reward".equalsIgnoreCase(bonusConfig.getImpact())) {
                totalReward = totalReward * bonusConfig.getRewardMultiplier();
            } else if ("extra_bonus".equalsIgnoreCase(bonusConfig.getImpact())) {
                totalReward = totalReward + bonusConfig.getExtra();
            }
        } else {
            totalReward = 0;
        }

        //Format the output result.
        GameResult result = new GameResult();
        List<List<String>> matrixList = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            List<String> rowList = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                rowList.add(matrix[r][c]);
            }
            matrixList.add(rowList);
        }
        result.matrix = matrixList;
        result.reward = (int) totalReward;
        result.setAppliedWinningCombinations(appliedWinningCombinations);
        result.setAppliedBonusSymbol((totalReward > 0 &&
                gameConfig.getSymbols().containsKey(bonusSymbol) &&
                !"miss".equalsIgnoreCase(gameConfig.getSymbols().get(bonusSymbol).getImpact()))
                ? bonusSymbol : null);
        return result;
    }

    private String sampleFromProbability(Map<String, Integer> probMap) {
        int total = probMap.values().stream().mapToInt(Integer::intValue).sum();
        int rand = random.nextInt(total) + 1;
        int cumulative = 0;
        for (Map.Entry<String, Integer> entry : probMap.entrySet()) {
            cumulative += entry.getValue();
            if (rand <= cumulative) {
                return entry.getKey();
            }
        }
        return probMap.keySet().iterator().next();
    }

}
