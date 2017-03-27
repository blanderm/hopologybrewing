package com.hopologybrewing.bcs.capture.service;

import java.util.ArrayList;
import java.util.List;
import com.hopologybrewing.bcs.capture.model.Process;

/**
 * Created by ddcbryanl on 12/20/16.
 */
public class ProcessService extends BcsService {
    public Process getProcess(String processId) {
        return (Process) getData(Type.PROCESS, processId);
    }

    public List getProcessStatus() {
        return (List) getData(Type.PROCESSES);
    }

    public List<Process> getProcesses() {
        List<Process> processes = new ArrayList<Process>();
        List processState = getProcessStatus();

        Process p;
        for (int i = 0; i < processState.size(); i++) {
            p = getProcess(String.valueOf(i));
            processes.add(p);
        }

        return processes;
    }
}
