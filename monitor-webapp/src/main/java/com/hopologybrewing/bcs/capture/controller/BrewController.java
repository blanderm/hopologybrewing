package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.service.BrewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
public class BrewController {
    private static final Logger log = LoggerFactory.getLogger(BrewController.class);
    private BrewService brewService;

    @RequestMapping("/brews")
    public HttpEntity<List<BrewInfo>> getBrews() {
        List<BrewInfo> brews = brewService.getBrews();
//        StringBuffer buffer = new StringBuffer("[");
//
//        Iterator<BrewInfo> itr = brews.iterator();
//        while (itr.hasNext()) {
//            buffer.append("\"").append(itr.next().getName()).append("\"");
//
//            if (itr.hasNext()) {
//                buffer.append(",");
//            }
//        }
//
//        buffer.append("]");
//
//        return new HttpEntity<String>(buffer.toString());
        return new HttpEntity<List<BrewInfo>>(brews);
    }

    @Autowired
    public void setBrewService(BrewService brewService) {
        this.brewService = brewService;
    }
}
