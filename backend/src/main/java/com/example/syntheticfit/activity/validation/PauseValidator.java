package com.example.syntheticfit.activity.validation;

import com.example.syntheticfit.activity.domain.PauseDefinition;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class PauseValidator {

    public void validate(List<PauseDefinition> pauses, Duration movingDuration) {
        if (pauses == null || pauses.isEmpty()) return;

        List<PauseDefinition> sorted = new ArrayList<>(pauses);
        sorted.sort(Comparator.comparing(PauseDefinition::offsetFromStart));

        for (PauseDefinition pause : sorted) {
            Duration pauseEnd = pause.offsetFromStart().plus(pause.duration());
            if (pauseEnd.compareTo(movingDuration) > 0) {
                throw new IllegalArgumentException(
                        "Pause at offset " + pause.offsetFromStart() + " extends beyond activity duration");
            }
        }

        for (int i = 1; i < sorted.size(); i++) {
            PauseDefinition prev = sorted.get(i - 1);
            PauseDefinition curr = sorted.get(i);
            Duration prevEnd = prev.offsetFromStart().plus(prev.duration());
            if (prevEnd.compareTo(curr.offsetFromStart()) > 0) {
                throw new IllegalArgumentException("Pauses overlap at offset " + curr.offsetFromStart());
            }
        }
    }
}
