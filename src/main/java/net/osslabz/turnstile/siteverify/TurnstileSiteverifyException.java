package net.osslabz.turnstile.siteverify;

public class TurnstileSiteverifyException extends RuntimeException {

    public TurnstileSiteverifyException(String message) {
        super(message);
    }

    public TurnstileSiteverifyException(Exception e) {
        super(e);
    }
}