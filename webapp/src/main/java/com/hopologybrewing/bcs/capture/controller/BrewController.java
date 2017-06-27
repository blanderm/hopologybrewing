package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.service.BrewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class BrewController {
    private static final Logger log = LoggerFactory.getLogger(BrewController.class);
    private BrewService brewService;

    @RequestMapping("/brews")
    public HttpEntity<Map<String, Object>> getBrews() {
        Map<String, Object> brewMap = new HashMap<>();

        List<BrewInfo> brews = brewService.getBrews();
        brewMap.put("brews", brews);
        brewMap.put("mostRecent", BrewInfo.getMostRecentBrewIndex(brews));

        return new HttpEntity<Map<String, Object>>(brewMap);
    }

    @Autowired
    public void setBrewService(BrewService brewService) {
        this.brewService = brewService;
    }
}
