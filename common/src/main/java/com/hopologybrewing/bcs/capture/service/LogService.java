package com.hopologybrewing.bcs.capture.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ddcbryanl on 4/2/17.
 */
public class LogService {
    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private Map<FileType, String> fileLocations = new HashMap<>();

    public void clearLogFile(FileType type) {
        PrintWriter writer = null;

        try {
            writer = new PrintWriter(fileLocations.get(type));
            writer.print("");
            writer.close();
        } catch (Exception e) {
            log.error("Failed clearing file " + fileLocations.get(type));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public void setOutputFile(String outputFile) {
        fileLocations.put(FileType.OUTPUT, outputFile);
    }

    public void setTempFile(String tempFile) {
        fileLocations.put(FileType.TEMP, tempFile);
    }

    public enum FileType {TEMP, OUTPUT}
}
