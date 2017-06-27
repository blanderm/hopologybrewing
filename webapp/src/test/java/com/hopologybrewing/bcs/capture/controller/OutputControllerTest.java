package com.hopologybrewing.bcs.capture.controller;

import com.hopologybrewing.bcs.capture.service.OutputService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
@EnableAutoConfiguration
public class OutputControllerTest {
    private OutputService outputService;

    @Test
    public void getProbeDataForBrew() throws Exception {
        OutputController controller = new OutputController();
        controller.setOutputService(outputService);
        Date upper = new Date();
        Date lower = new Date();
        lower.setDate(lower.getDate() - 3);
        HttpEntity<String> response = controller.getHistoricalOutputs(0L, lower.getTime(), upper.getTime());
        Assert.assertNotNull("Data point size not 0", response.getBody());
    }

    @Autowired
    public void setOutputService(OutputService outputService) {
        this.outputService = outputService;
    }
}