package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.model.Process;
import com.hopologybrewing.bcs.capture.model.State;
import com.hopologybrewing.bcs.capture.service.StateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by ddcbryanl on 12/20/16.
 */
@RestController
public class StateController {
    private StateService stateService;

    @RequestMapping("/process/{pid}/state/{sid}")
    public HttpEntity<State> getState(@PathVariable String pid, @PathVariable String sid) {
        return new HttpEntity<State>(stateService.getState(pid, sid));
    }

    @RequestMapping("/process/{pid}/current_state")
    public HttpEntity<Process> getCurrentState(@PathVariable String pid) {
        return new HttpEntity<Process>(stateService.getCurrentProcessState(pid));
    }

    @Autowired
    public void setStateService(StateService stateService) {
        this.stateService = stateService;
    }
}
