package com.example.syntheticfit.fit.validation;

import com.garmin.fit.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class GarminFitActivityValidator implements FitActivityValidator {

    @Override
    public FitValidationResult validate(byte[] fitFile) {
        List<String> issues = new ArrayList<>();

        if (fitFile == null || fitFile.length == 0) {
            return FitValidationResult.failed(List.of("FIT file is empty"));
        }

        // FIT files must be at least 14 bytes (header)
        if (fitFile.length < 14) {
            return FitValidationResult.failed(List.of("FIT file too small: " + fitFile.length + " bytes"));
        }

        // Decode and collect messages
        MessageCollector collector = new MessageCollector();

        try {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
            broadcaster.addListener((RecordMesgListener) collector);
            broadcaster.addListener((SessionMesgListener) collector);
            broadcaster.addListener((ActivityMesgListener) collector);
            broadcaster.addListener((LapMesgListener) collector);
            broadcaster.addListener((EventMesgListener) collector);

            boolean integrityOk = decode.checkFileIntegrity(new ByteArrayInputStream(fitFile));
            if (!integrityOk) {
                issues.add("FIT integrity check failed (invalid CRC or header)");
            }

            broadcaster.run(new ByteArrayInputStream(fitFile));
        } catch (Exception e) {
            issues.add("FIT decode error: " + e.getMessage());
            return FitValidationResult.failed(issues);
        }

        // Check required messages
        if (collector.records.isEmpty()) issues.add("No Record messages found");
        if (collector.sessions.isEmpty()) issues.add("No Session message found");
        if (collector.activities.isEmpty()) issues.add("No Activity message found");
        if (collector.laps.isEmpty()) issues.add("No Lap message found");

        // Validate record ordering and distance
        if (!collector.records.isEmpty()) {
            Double prevDist = null;
            DateTime prevTs = null;
            for (RecordMesg r : collector.records) {
                if (r.getDistance() != null) {
                    if (prevDist != null && r.getDistance() < prevDist - 0.01) {
                        issues.add("Distance decreased: " + prevDist + " -> " + r.getDistance());
                    }
                    prevDist = r.getDistance().doubleValue();
                }
                if (r.getTimestamp() != null) {
                    if (prevTs != null && r.getTimestamp().compareTo(prevTs) < 0) {
                        issues.add("Timestamp not monotonically increasing");
                    }
                    prevTs = r.getTimestamp();
                }
            }
        }

        return issues.isEmpty() ? FitValidationResult.ok() : FitValidationResult.failed(issues);
    }

    private static class MessageCollector
            implements RecordMesgListener, SessionMesgListener,
            ActivityMesgListener, LapMesgListener, EventMesgListener {
        final List<RecordMesg> records = new ArrayList<>();
        final List<SessionMesg> sessions = new ArrayList<>();
        final List<ActivityMesg> activities = new ArrayList<>();
        final List<LapMesg> laps = new ArrayList<>();
        final List<EventMesg> events = new ArrayList<>();

        @Override public void onMesg(RecordMesg m) { records.add(m); }
        @Override public void onMesg(SessionMesg m) { sessions.add(m); }
        @Override public void onMesg(ActivityMesg m) { activities.add(m); }
        @Override public void onMesg(LapMesg m) { laps.add(m); }
        @Override public void onMesg(EventMesg m) { events.add(m); }
    }
}
