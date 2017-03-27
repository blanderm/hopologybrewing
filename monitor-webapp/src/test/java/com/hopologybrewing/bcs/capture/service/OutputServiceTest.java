package com.hopologybrewing.bcs.capture.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
public class OutputServiceTest {
    private OutputService outputService;

    @Test
    public void getHistoricalOutputData() throws Exception {
        outputService.getHistoricalOutputData(0L, 5000000000000L, 200);
    }

    @Autowired
    public void setOutputService(OutputService outputService) {
        this.outputService = outputService;
    }
}