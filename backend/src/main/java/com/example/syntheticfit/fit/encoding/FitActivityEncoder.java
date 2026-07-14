package com.example.syntheticfit.fit.encoding;

import com.example.syntheticfit.activity.domain.*;
import com.garmin.fit.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class FitActivityEncoder {

    private static final long GARMIN_EPOCH_OFFSET = 631065600L;
    private static final int SYNTHETIC_PRODUCT = 9999;
    private static final long SYNTHETIC_SERIAL = 12345678L;

    public byte[] encode(GeneratedActivity activity) {
        BufferEncoder encoder = new BufferEncoder(Fit.ProtocolVersion.V2_0);

        ActivitySummary summary = activity.summary();
        List<ActivitySample> samples = activity.samples();
        SportType sport = activity.sport();

        writeFileId(encoder, summary);
        writeTimerStart(encoder, summary.startTime());

        boolean inPause = false;
        for (int i = 0; i < samples.size(); i++) {
            ActivitySample sample = samples.get(i);
            boolean nowPaused = sample.isPaused();

            if (!inPause && nowPaused) {
                writeTimerEvent(encoder, sample.timestamp(), EventType.STOP_ALL);
                inPause = true;
            } else if (inPause && !nowPaused && i > 0) {
                writeTimerEvent(encoder, sample.timestamp(), EventType.START);
                inPause = false;
            }

            writeRecord(encoder, sample);
        }

        writeTimerEvent(encoder, summary.endTime(), EventType.STOP_ALL);
        writeLap(encoder, summary, sport);
        writeSession(encoder, summary, sport);
        writeActivity(encoder, summary);

        return encoder.close();
    }

    private void writeFileId(BufferEncoder encoder, ActivitySummary summary) {
        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(File.ACTIVITY);
        fileId.setManufacturer(Manufacturer.GARMIN);
        fileId.setProduct(SYNTHETIC_PRODUCT);
        fileId.setSerialNumber(SYNTHETIC_SERIAL);
        fileId.setTimeCreated(toGarminDateTime(summary.startTime()));
        encoder.write(fileId);
    }

    private void writeTimerStart(BufferEncoder encoder, Instant time) {
        EventMesg event = new EventMesg();
        event.setTimestamp(toGarminDateTime(time));
        event.setEvent(Event.TIMER);
        event.setEventType(EventType.START);
        encoder.write(event);
    }

    private void writeTimerEvent(BufferEncoder encoder, Instant time, EventType type) {
        EventMesg event = new EventMesg();
        event.setTimestamp(toGarminDateTime(time));
        event.setEvent(Event.TIMER);
        event.setEventType(type);
        encoder.write(event);
    }

    private void writeRecord(BufferEncoder encoder, ActivitySample sample) {
        RecordMesg record = new RecordMesg();
        record.setTimestamp(toGarminDateTime(sample.timestamp()));
        record.setPositionLat(degreesToSemicircles(sample.latitude()));
        record.setPositionLong(degreesToSemicircles(sample.longitude()));
        record.setDistance((float) sample.distanceMeters());
        record.setEnhancedSpeed((float) sample.speedMetersPerSecond());
        record.setHeartRate((short) sample.heartRate());
        if (sample.altitudeMeters() != null) {
            record.setEnhancedAltitude((float) sample.altitudeMeters().doubleValue());
        }
        if (sample.cadenceSpm() != null) {
            // FIT cadence for running is strides/min (half of steps/min), but
            // Garmin Connect displays it as steps/min — store raw spm value here
            // and let the FIT field carry it directly.
            record.setCadence(sample.cadenceSpm().shortValue());
        }
        encoder.write(record);
    }

    private void writeLap(BufferEncoder encoder, ActivitySummary summary, SportType sport) {
        LapMesg lap = new LapMesg();
        lap.setTimestamp(toGarminDateTime(summary.endTime()));
        lap.setStartTime(toGarminDateTime(summary.startTime()));
        lap.setTotalElapsedTime((float) summary.elapsedDurationSeconds());
        lap.setTotalTimerTime((float) summary.movingDurationSeconds());
        lap.setTotalDistance((float) summary.distanceMeters());
        lap.setAvgSpeed((float) summary.averageSpeedMps());
        lap.setMaxSpeed((float) summary.maxSpeedMps());
        lap.setAvgHeartRate((short) summary.averageHeartRate());
        lap.setMaxHeartRate((short) summary.maxHeartRate());
        lap.setTotalCalories(summary.totalCalories());
        lap.setEvent(Event.LAP);
        lap.setEventType(EventType.STOP);
        lap.setSport(toFitSport(sport));
        encoder.write(lap);
    }

    private void writeSession(BufferEncoder encoder, ActivitySummary summary, SportType sport) {
        SessionMesg session = new SessionMesg();
        session.setTimestamp(toGarminDateTime(summary.endTime()));
        session.setStartTime(toGarminDateTime(summary.startTime()));
        session.setTotalElapsedTime((float) summary.elapsedDurationSeconds());
        session.setTotalTimerTime((float) summary.movingDurationSeconds());
        session.setTotalDistance((float) summary.distanceMeters());
        session.setAvgSpeed((float) summary.averageSpeedMps());
        session.setMaxSpeed((float) summary.maxSpeedMps());
        session.setAvgHeartRate((short) summary.averageHeartRate());
        session.setMaxHeartRate((short) summary.maxHeartRate());
        session.setTotalCalories(summary.totalCalories());
        session.setSport(toFitSport(sport));
        session.setSubSport(toFitSubSport(sport));
        session.setEvent(Event.SESSION);
        session.setEventType(EventType.STOP);
        session.setFirstLapIndex(0);
        session.setNumLaps(1);
        session.setTrigger(SessionTrigger.ACTIVITY_END);
        encoder.write(session);
    }

    private void writeActivity(BufferEncoder encoder, ActivitySummary summary) {
        ActivityMesg activityMesg = new ActivityMesg();
        activityMesg.setTimestamp(toGarminDateTime(summary.endTime()));
        activityMesg.setTotalTimerTime((float) summary.movingDurationSeconds());
        activityMesg.setNumSessions(1);
        activityMesg.setType(Activity.MANUAL);
        activityMesg.setEvent(Event.ACTIVITY);
        activityMesg.setEventType(EventType.STOP);
        encoder.write(activityMesg);
    }

    private Sport toFitSport(SportType sport) {
        return sport == SportType.RUNNING ? Sport.RUNNING : Sport.CYCLING;
    }

    private SubSport toFitSubSport(SportType sport) {
        return sport == SportType.RUNNING ? SubSport.STREET : SubSport.ROAD;
    }

    private DateTime toGarminDateTime(Instant instant) {
        long garminSeconds = instant.getEpochSecond() - GARMIN_EPOCH_OFFSET;
        return new DateTime(garminSeconds);
    }

    private int degreesToSemicircles(double degrees) {
        return (int) Math.round(degrees * (Math.pow(2, 31) / 180.0));
    }
}
