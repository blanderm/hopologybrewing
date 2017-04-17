package com.hopologybrewing.bcs.capture.aws.dynamo;

import com.hopologybrewing.bcs.capture.model.Output;
import com.hopologybrewing.bcs.capture.model.OutputRecording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbe;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;

import java.util.HashMap;
import java.util.Map;

import static com.hopologybrewing.bcs.capture.model.OutputRecording.OUTPUT_TYPE;
import static com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording.TEMPERATURE_TYPE;

/**
 * Created by ddcbryanl on 1/18/17.
 */
public class DynamoConstants {
    public static final String BEER_FERMENTATION_TABLE = "beer_fermentation";
    public static final String BREW_READINGS_TABLE = "brew_readings";
    public static final String BREW_INFO_TABLE = "brew_info";
    public static final Map<String, Class> tableRecordingMap = new HashMap<>();
    public static final Map<Class, String> recordingClassTypeMap = new HashMap<>();
    public static final Map<String, Class> typeClassMap = new HashMap<>();
    public static final Map<String, Class> typeRecordingClassMap = new HashMap<>();

    static {
        tableRecordingMap.put(TEMPERATURE_TYPE, TemperatureProbeRecording.class);
        tableRecordingMap.put(OUTPUT_TYPE, OutputRecording.class);

        recordingClassTypeMap.put(TemperatureProbeRecording.class, TEMPERATURE_TYPE);
        recordingClassTypeMap.put(OutputRecording.class, OUTPUT_TYPE);

        typeClassMap.put(TEMPERATURE_TYPE, TemperatureProbe.class);
        typeClassMap.put(OUTPUT_TYPE, Output.class);

        typeRecordingClassMap.put(TEMPERATURE_TYPE, TemperatureProbeRecording.class);
        typeRecordingClassMap.put(OUTPUT_TYPE, OutputRecording.class);
    }
}
