package com.hopologybrewing.bcs.capture.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Output;
import com.hopologybrewing.bcs.capture.service.OutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestController
public class OutputController {
    private static final Logger log = LoggerFactory.getLogger(OutputController.class);
    private OutputService outputService;

    @RequestMapping("/output/{oid}")
    public HttpEntity<Output> getOutput(@PathVariable String oid) {
        return new HttpEntity<Output>(outputService.getOutput(oid));
    }

    @RequestMapping("/output")
    public HttpEntity<List<Output>> getEnabledOutputs() {
        return new HttpEntity<List<Output>>(outputService.getEnabledOutputs());
    }

//    @RequestMapping("/output/history")
//    public HttpEntity<String> getHistoricalTemps() {
//        return getHistoricalTemps(0L);
//    }

    @RequestMapping("/output/history/{date}")
    public HttpEntity<String> getHistoricalOutputs(@PathVariable long date, @RequestParam(value="lowerRange") long lowerRange, @RequestParam(value="upperRange") long upperRange) {
        StringBuffer buffer = new StringBuffer();
        Map<String, List<List>> probesMap = null;

        if (date == 0L) {
            probesMap = outputService.getProbeDataForBrew(lowerRange, upperRange);
        } else {
            probesMap = outputService.getProbeDataForBrew(new Date(date), lowerRange, upperRange);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            buffer.append("[");
            String name;
            for (Iterator<String> it = probesMap.keySet().iterator(); it.hasNext(); ) {
                name = it.next();
                buffer.append("{\"name\": \"").append(name).append("\", \"data\":")
                        .append(mapper.writeValueAsString(probesMap.get(name))).append("}");

                if (it.hasNext()) {
                    buffer.append(",");
                }
            }

            buffer.append("]");
        } catch (JsonProcessingException e) {
            log.error("Failed to convert the result to json - ", e);
        }

        return new HttpEntity<String>(buffer.toString());
    }

    @Autowired
    public void setOutputService(OutputService outputService) {
        this.outputService = outputService;
    }
}
