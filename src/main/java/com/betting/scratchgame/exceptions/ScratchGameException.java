package com.betting.scratchgame.exceptions;

public class ScratchGameException extends RuntimeException{
    private String reason;

    private Throwable throwable;

    public ScratchGameException(String reason, Throwable throwable) {
        this.reason = reason;
        this.throwable = throwable;
    }
}
