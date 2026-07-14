package com.example.syntheticfit.api;

import java.util.List;

public class FitEncodingException extends RuntimeException {
    private final String code;
    private final List<String> details;

    public FitEncodingException(String code, String message, List<String> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public String getCode() { return code; }
    public List<String> getDetails() { return details; }
}
