package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.service.TemperatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AlertController {
    private static final Logger log = LoggerFactory.getLogger(AlertController.class);
    private TemperatureService tempService;

    @RequestMapping("/alert/status")
    public HttpEntity<Boolean> getStatus() {
        return new HttpEntity<Boolean>(tempService.getAlertStatus());
    }

    @RequestMapping("/alert/toggle")
    public HttpEntity<String> updateAlert() {
        log.info("Toggling alert");
        tempService.toggleAlerting();

        return new HttpEntity<String>("Success");
    }

    @RequestMapping("/alert/test")
    public HttpEntity<String> sendTestMessage() {
        tempService.sendTestMessage();
        return new HttpEntity<String>("Success");
    }

    @Autowired
    public void setTempService(TemperatureService tempService) {
        this.tempService = tempService;
    }
}
