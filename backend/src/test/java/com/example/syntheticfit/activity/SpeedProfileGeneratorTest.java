package com.example.syntheticfit.activity;

import com.example.syntheticfit.activity.generation.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class SpeedProfileGeneratorTest {

    private final RealisticSpeedProfileGenerator generator = new RealisticSpeedProfileGenerator();

    @Test
    void generatedProfileMatchesRouteDistance() {
        SpeedProfileRequest request = new SpeedProfileRequest(
                10000, 10.0, 1000, 1, 42L, List.of());
        SpeedProfile profile = generator.generate(request);

        double totalDist = profile.speedsAtEachSample().stream()
                .mapToDouble(s -> s * 1).sum(); // interval=1s
        assertThat(totalDist).isCloseTo(10000, within(50.0));
    }

    @Test
    void sameSeadProducesSameResult() {
        SpeedProfileRequest req = new SpeedProfileRequest(5000, 5.0, 1000, 1, 999L, List.of());
        SpeedProfile p1 = generator.generate(req);
        SpeedProfile p2 = generator.generate(req);
        assertThat(p1.speedsAtEachSample()).isEqualTo(p2.speedsAtEachSample());
    }

    @Test
    void differentSeedsProduceDifferentResults() {
        SpeedProfileRequest r1 = new SpeedProfileRequest(5000, 5.0, 1000, 1, 1L, List.of());
        SpeedProfileRequest r2 = new SpeedProfileRequest(5000, 5.0, 1000, 1, 2L, List.of());
        SpeedProfile p1 = generator.generate(r1);
        SpeedProfile p2 = generator.generate(r2);
        assertThat(p1.speedsAtEachSample()).isNotEqualTo(p2.speedsAtEachSample());
    }
}
