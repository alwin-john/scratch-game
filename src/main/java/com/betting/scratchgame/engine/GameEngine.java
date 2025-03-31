package com.betting.scratchgame.engine;

import com.betting.scratchgame.models.GameResult;
import com.betting.scratchgame.models.GeneratedMatrixInfo;

public interface GameEngine {
    public GeneratedMatrixInfo instantiateBoard();
    public GameResult play(GeneratedMatrixInfo matrixInfo);
}
