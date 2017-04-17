package com.hopologybrewing.bcs.capture.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
public class TemperatureServiceTest {
    private TemperatureService temperatureService;

    @Test
    public void getProbeDataForBrew() throws Exception {
        Map<String, List<List>> map = temperatureService.getProbeDataForBrew();
        Assert.assertNotEquals("Data point size not 0", 0, map.size());
    }

    @Autowired
    public void setTemperatureService(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }
}