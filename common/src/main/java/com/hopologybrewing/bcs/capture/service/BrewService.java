package com.hopologybrewing.bcs.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbe;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class BrewService {
    private static final Logger log = LoggerFactory.getLogger(BrewService.class);
    private DbService dbService;

    public List<BrewInfo> getBrews() {
        return dbService.getAllBrews();
    }

    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }
}
