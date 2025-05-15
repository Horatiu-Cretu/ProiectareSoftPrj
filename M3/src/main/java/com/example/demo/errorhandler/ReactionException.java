package com.example.demo.errorhandler;


public class ReactionException extends DemoException {
    public ReactionException(String message) {
        super(message);
    }

    public ReactionException (String message, Throwable cause) {
        super(message, cause);
    }
}