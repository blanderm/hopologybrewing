package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.BasicAuthRestTemplate;
import com.hopologybrewing.bcs.capture.model.*;
import com.hopologybrewing.bcs.capture.model.Process;
import com.hopologybrewing.bcs.capture.model.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public abstract class BcsService {
    private static final Logger log = LoggerFactory.getLogger(BcsService.class);
    private static Map<Type, String> urlMap = null;
    private static Map<Type, Class> classMap = null;
    private RestTemplate template;
    private String bcsIp;

    static {
        Map<Type, String> url = new HashMap<>();
        url.put(Type.TEMP, BcsConstants.API_ROOT + "temp/%s");
        url.put(Type.TEMPS, BcsConstants.API_ROOT + "temp");
        url.put(Type.PROCESS, BcsConstants.API_ROOT + "process/%s");
        url.put(Type.PROCESSES, BcsConstants.API_ROOT + "process");
        url.put(Type.STATE, BcsConstants.API_ROOT + "process/%s/state/%s");
        url.put(Type.TIMER, BcsConstants.API_ROOT + "process/%s/timer/%s");
        url.put(Type.OUTPUT, BcsConstants.API_ROOT + "output/%s");
        url.put(Type.OUTPUTS, BcsConstants.API_ROOT + "output");
        url.put(Type.EXIT_CONDITIONS, BcsConstants.API_ROOT + "/process/%s/state/%s/exit_conditions");
        urlMap = Collections.unmodifiableMap(url);

        Map<Type, Class> clz = new HashMap<>();
        clz.put(Type.TEMP, TemperatureProbe.class);
        clz.put(Type.TEMPS, List.class);
        clz.put(Type.PROCESS, Process.class);
        clz.put(Type.PROCESSES, List.class);
        clz.put(Type.STATE, State.class);
        clz.put(Type.TIMER, Timer.class);
        clz.put(Type.OUTPUT, Output.class);
        clz.put(Type.OUTPUTS, ArrayList.class);
        clz.put(Type.EXIT_CONDITIONS, ArrayList.class);
        classMap = Collections.unmodifiableMap(clz);
    }

    private AtomicBoolean alertEnabled = new AtomicBoolean(true);

    public BcsService() {
        bcsIp = System.getProperty(BcsConstants.BCS_IP);
        if (StringUtils.isEmpty(bcsIp)) {
            bcsIp = System.getenv(BcsConstants.BCS_IP);
        }
    }

    public BcsService(String user, String pwd) {
        bcsIp = System.getProperty(BcsConstants.BCS_IP);
        if (StringUtils.isEmpty(bcsIp)) {
            bcsIp = System.getenv(BcsConstants.BCS_IP);
        }

        this.template = new BasicAuthRestTemplate(user, pwd);
    }

    public BcsService(String user, String pwd, String ip) {
        this.template = new BasicAuthRestTemplate(user, pwd);
        bcsIp = ip;
    }

    public Object getData(Type type, String... ids) {
        Object obj = null;
        ResponseEntity response = null;

        try {
            RestTemplate template = new BasicAuthRestTemplate();
            String bcsIp = System.getProperty(BcsConstants.BCS_IP);
            response = template.getForEntity("http://" + bcsIp + String.format(urlMap.get(type), ids), classMap.get(type));
            obj = response.getBody();
        } catch (Throwable t) {
            log.error("Error getting data - ", t);
        }

        return obj;
    }

    public void toggleAlerting() {
        boolean currentState = alertEnabled.get();
        log.info("Setting alerting to " + (!currentState ? "on" : "off"));
        alertEnabled.set(!currentState);
    }

    public boolean getAlertStatus() {
        return alertEnabled.get();
    }

    public enum Type {TEMP, TEMPS, PROCESS, PROCESSES, STATE, TIMER, OUTPUT, OUTPUTS, EXIT_CONDITIONS}
}
