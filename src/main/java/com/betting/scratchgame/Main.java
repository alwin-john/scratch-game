package com.betting.scratchgame;

import com.betting.scratchgame.engine.GameEngineImpl;
import com.betting.scratchgame.exceptions.ScratchGameException;
import com.betting.scratchgame.models.GameConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar your-scratch-game.jar --config config.json --betting-amount 100");
            return;
        }

        String configFile = "";
        int betAmount = 0;
        for (int i = 0; i < args.length; i++) {
            if ("--config".equals(args[i])) {
                configFile = args[i + 1];
            } else if ("--betting-amount".equals(args[i])) {
                betAmount = Integer.parseInt(args[i + 1]);
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            GameConfig config = mapper.readValue(new File("config.json"), GameConfig.class);
            GameEngineImpl engine = new GameEngineImpl(config, betAmount);
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(engine.play(engine.instantiateBoard())));
        } catch (IOException e) {
            System.out.println("Invalid config or config not found");
            throw new ScratchGameException("Invalid Config", e);
        }
    }
}