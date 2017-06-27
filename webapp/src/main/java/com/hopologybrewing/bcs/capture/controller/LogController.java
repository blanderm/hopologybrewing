package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogController {
    private static final Logger log = LoggerFactory.getLogger(LogController.class);
    private LogService logService;

    @RequestMapping("/log/clear")
    public HttpEntity<String> clearLog(@RequestParam String type) {
        log.info("Clearing log " + type);
        LogService.FileType fileType = ("output".equalsIgnoreCase(type)) ? LogService.FileType.OUTPUT : LogService.FileType.TEMP;
        logService.clearLogFile(fileType);

        return new HttpEntity<String>("Success");
    }

    @Autowired
    public void setLogService(LogService logService) {
        this.logService = logService;
    }
}
