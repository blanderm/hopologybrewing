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
    public static final String BREW_INFO_TABLE = "brew_info";
    public static final String BREW_RECORDINGS_TABLE = "brew_recordings";
    public static final String BREW_RECORDINGS_TIMESTAMP = "timestamp";
    public static final String BREW_RECORDINGS_TYPE = "type";
    public static final String BREW_RECORDINGS_DATA = "data";
    public static final String BREW_RECORDINGS_BREW_DATE = "brew_date";
}
