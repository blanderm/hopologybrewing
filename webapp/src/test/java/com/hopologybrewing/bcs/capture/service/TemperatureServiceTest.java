package com.hopologybrewing.bcs.capture.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
@EnableAutoConfiguration
public class TemperatureServiceTest {
    private TemperatureService temperatureService;

    @Test
    public void getProbeDataForBrew() throws Exception {
        Date upper = new Date();
        Date lower = new Date();
        lower.setDate(lower.getDate() - 3);
        Map<String, List<List>> map = temperatureService.getProbeDataForBrew(lower.getTime(), upper.getTime());
        Assert.assertNotEquals("Data point size not 0", 0, map.size());
    }

    @Autowired
    public void setTemperatureService(TemperatureService temperatureService) {
        this.temperatureService = temperatureService;
    }
}