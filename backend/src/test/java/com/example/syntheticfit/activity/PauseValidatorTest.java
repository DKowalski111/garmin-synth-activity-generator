package com.example.syntheticfit.activity;

import com.example.syntheticfit.activity.domain.PauseDefinition;
import com.example.syntheticfit.activity.validation.PauseValidator;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PauseValidatorTest {

    private final PauseValidator validator = new PauseValidator();

    @Test
    void validPauses_passValidation() {
        List<PauseDefinition> pauses = List.of(
                new PauseDefinition(Duration.ofSeconds(100), Duration.ofSeconds(60)),
                new PauseDefinition(Duration.ofSeconds(300), Duration.ofSeconds(30)));
        assertThatNoException().isThrownBy(() ->
                validator.validate(pauses, Duration.ofSeconds(600)));
    }

    @Test
    void overlappingPauses_throwException() {
        List<PauseDefinition> pauses = List.of(
                new PauseDefinition(Duration.ofSeconds(100), Duration.ofSeconds(100)),
                new PauseDefinition(Duration.ofSeconds(150), Duration.ofSeconds(50)));
        assertThatThrownBy(() ->
                validator.validate(pauses, Duration.ofSeconds(600)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void pauseBeyondActivity_throwException() {
        List<PauseDefinition> pauses = List.of(
                new PauseDefinition(Duration.ofSeconds(500), Duration.ofSeconds(200)));
        assertThatThrownBy(() ->
                validator.validate(pauses, Duration.ofSeconds(600)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
