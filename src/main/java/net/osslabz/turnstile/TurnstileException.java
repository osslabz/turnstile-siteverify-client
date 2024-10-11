package net.osslabz.turnstile;

public class TurnstileException extends RuntimeException {

    public TurnstileException(String message) {
        super(message);
    }

    public TurnstileException(Exception e) {
        super(e);
    }
}
