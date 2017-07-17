package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by ddcbryanl on 1/27/17.
 */
public interface DbService {

    // todo: create an interface that allows brew creation/initiation and edit
    BrewInfo getCurrentBrew() throws ExecutionException;

    Date getCurrentBrewDate() throws ExecutionException;

    Date getMostRecentBrewDate() throws ExecutionException;

    BrewInfo createBrew(BrewInfo newBrew) throws Exception;

    List<BrewInfo> getAllBrews() throws ExecutionException;

    List<Recording> findTemperatureReadings(Date brewDate, long lowerRange, long upperRange);

    List<Recording> findOutputReadings(Date brewDate, long lowerRange, long upperRange);

    void saveReadings(List<Recording> message);
}
