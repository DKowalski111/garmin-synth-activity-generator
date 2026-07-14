package com.example.syntheticfit.fit.validation;

import java.util.List;

public record FitValidationResult(boolean valid, List<String> issues) {
    public static FitValidationResult ok() {
        return new FitValidationResult(true, List.of());
    }

    public static FitValidationResult failed(List<String> issues) {
        return new FitValidationResult(false, issues);
    }
}
