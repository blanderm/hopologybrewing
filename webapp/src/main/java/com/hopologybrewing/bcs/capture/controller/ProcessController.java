package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.model.Process;
import com.hopologybrewing.bcs.capture.service.BcsService;
import com.hopologybrewing.bcs.capture.service.ProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by ddcbryanl on 12/6/16.
 */
@RestController
public class ProcessController extends BcsService {
    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);
    private ProcessService processService;

    @RequestMapping("/process/{pid}")
    public HttpEntity<Process> getProcess(@PathVariable String pid) {
        return new HttpEntity<Process>(processService.getProcess(pid));
    }

    @RequestMapping("/process")
    public HttpEntity<List> getProcessStatus() {
        return new HttpEntity<List>(processService.getProcesses());
    }

    @RequestMapping("/process/status")
    public HttpEntity<List> getProcesses() {
        return new HttpEntity<List>(processService.getProcessStatus());
    }

    @Autowired
    public void setProcessService(ProcessService processService) {
        this.processService = processService;
    }
}
