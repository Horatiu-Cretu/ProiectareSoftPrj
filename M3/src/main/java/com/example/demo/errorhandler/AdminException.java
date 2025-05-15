package com.example.demo.errorhandler;


public class AdminException extends DemoException {
    public AdminException(String message) {
        super(message);
    }

    public AdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
