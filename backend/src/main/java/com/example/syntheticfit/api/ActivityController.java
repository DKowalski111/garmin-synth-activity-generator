package com.example.syntheticfit.api;

import com.example.syntheticfit.activity.domain.*;
import com.example.syntheticfit.activity.generation.ActivityGenerator;
import com.example.syntheticfit.fit.encoding.FitActivityEncoder;
import com.example.syntheticfit.fit.validation.FitActivityValidator;
import com.example.syntheticfit.fit.validation.FitValidationResult;
import com.example.syntheticfit.routing.domain.Route;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityGenerator activityGenerator;
    private final FitActivityEncoder fitEncoder;
    private final FitActivityValidator fitValidator;
    private final ActivityRequestMapper mapper;
    private final Clock clock;

    public ActivityController(
            ActivityGenerator activityGenerator,
            FitActivityEncoder fitEncoder,
            FitActivityValidator fitValidator,
            ActivityRequestMapper mapper,
            Clock clock) {
        this.activityGenerator = activityGenerator;
        this.fitEncoder = fitEncoder;
        this.fitValidator = fitValidator;
        this.mapper = mapper;
        this.clock = clock;
    }

    @PostMapping("/preview")
    public ResponseEntity<ActivityPreviewResponse> preview(@Valid @RequestBody ActivityApiRequest request) {
        Route route = mapper.toRoute(request.route());
        ActivityConfiguration config = mapper.toConfig(request);
        GeneratedActivity activity = activityGenerator.generate(route, config);

        ActivityPreviewResponse response = toPreviewResponse(activity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fit")
    public ResponseEntity<byte[]> generateFit(@Valid @RequestBody ActivityApiRequest request) {
        // Capture server timestamp at the very start for END_NOW mode
        Instant requestTime = clock.instant();

        Route route = mapper.toRoute(request.route());

        // Override fixedEndTime for END_NOW so time is captured at request start
        ActivityApiRequest withTimestamp = request.timeConfiguration().mode().equals("END_NOW")
                ? new ActivityApiRequest(
                        request.activityName(), request.sport(), request.route(),
                        request.timeConfiguration(), request.averageSpeedKmh(),
                        request.averageHeartRate(), request.recordingIntervalSeconds(),
                        request.seed(), request.pauses(), requestTime,
                        request.averagePaceMinPerKm(), request.cadenceSpm())
                : request;

        ActivityConfiguration config = mapper.toConfig(withTimestamp);
        GeneratedActivity activity = activityGenerator.generate(route, config);

        byte[] fitBytes = fitEncoder.encode(activity);

        FitValidationResult validation = fitValidator.validate(fitBytes);
        if (!validation.valid()) {
            throw new FitEncodingException("FIT_VALIDATION_FAILED",
                    "Generated FIT file failed validation", validation.issues());
        }

        String filename = sanitizeFilename(config.activityName()) + ".fit";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ant.fit"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(fitBytes);
    }

    private ActivityPreviewResponse toPreviewResponse(GeneratedActivity activity) {
        ActivitySummary s = activity.summary();
        ActivityPreviewResponse.SummaryDto summaryDto = new ActivityPreviewResponse.SummaryDto(
                s.startTime(), s.endTime(), s.distanceMeters(),
                s.movingDurationSeconds(), s.elapsedDurationSeconds(),
                s.totalPauseDurationSeconds(), s.averageSpeedMps(), s.maxSpeedMps(),
                s.averageHeartRate(), s.maxHeartRate(), s.sampleCount(), s.totalCalories()
        );

        List<ActivityPreviewResponse.SampleDto> sampleDtos = activity.samples().stream()
                .map(sample -> new ActivityPreviewResponse.SampleDto(
                        sample.timestamp(), sample.latitude(), sample.longitude(),
                        sample.distanceMeters(), sample.speedMetersPerSecond(),
                        sample.heartRate(), sample.isPaused(), sample.cadenceSpm()))
                .toList();

        return new ActivityPreviewResponse(activity.sport().name(), summaryDto, sampleDtos);
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "synthetic-cycling-activity";
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_").toLowerCase();
    }
}
