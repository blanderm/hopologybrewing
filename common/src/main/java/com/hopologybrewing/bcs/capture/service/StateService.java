package com.hopologybrewing.bcs.capture.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.ExitCondition;
import com.hopologybrewing.bcs.capture.model.Process;
import com.hopologybrewing.bcs.capture.model.State;
import com.hopologybrewing.bcs.capture.model.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ddcbryanl on 12/20/16.
 */
public class StateService extends BcsService {
    private static final Logger log = LoggerFactory.getLogger(StateService.class);

    public State getState(String processId, String stateId) {
        return (State) getData(Type.STATE, processId, stateId);
    }

    public Process getCurrentProcessState(String processId) {
        // get process to find active state
        Process p = (Process) getData(Type.PROCESS, processId);

        if (p.isRunning() && p.getCurrentState() != null) {
            Integer stateId = p.getCurrentState().getState();
            State state = (State) getData(Type.STATE, processId, String.valueOf(stateId));

            if (state.getTimers() != null) {
                Timer timer;
                Timer timerDetails;
                List<Timer> newTimerList = new ArrayList<Timer>();
                for (int i = 0; i < state.getTimers().size(); i++) {
                    timer = state.getTimers().get(i);

                    if (timer.isUsed()) {
                        timerDetails = (Timer) getData(Type.TIMER, processId, String.valueOf(i));
                        timer.setName(timerDetails.getName());
                        timer.setOn(timerDetails.isOn());
                        timer.setEnabled(timerDetails.isEnabled());
                        timer.setValue(timerDetails.getValue());
                        newTimerList.add(timer);
                    }
                }

                state.setTimers(newTimerList);
            }

            List exitConditions = (List) getData(Type.EXIT_CONDITIONS, processId, String.valueOf(stateId));

            if (exitConditions != null) {
                List<ExitCondition> enabledExitConditions = new ArrayList<>();
                ObjectMapper mapper = new ObjectMapper();
                ExitCondition ec = new ExitCondition();
                for (Object o : exitConditions) {
                    try {
                        // convert LinkedHashMap toString to json format #hack
                        String str = StringUtils.replace(o.toString(), "{", "{\"");
                        str = StringUtils.replace(str, "=", "\":");
                        str = StringUtils.replace(str, ", ", ", \"");
                        ec = (ExitCondition) mapper.readValue(str, ExitCondition.class);

                        if (ec.isEnabled()) {
                            enabledExitConditions.add(ec);
                        }
                    } catch (IOException e) {
                        log.error("Failed reading exit condition and converting from json to object - ", e);
                    }
                }

                state.setExitConditions(enabledExitConditions);
            }

            p.getStatesObj().set(stateId, state);
        }

        return p;
    }
}
