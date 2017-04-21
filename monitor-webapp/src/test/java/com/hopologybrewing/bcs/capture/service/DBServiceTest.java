package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.batch.TemperatureProbeMessageRecorder;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
@EnableAutoConfiguration
public class DBServiceTest {
    private DbService dbService;
    private TemperatureProbeMessageRecorder recorder;

    @Test
    public void getCurrentBeerDate() throws Exception {
        Date date = dbService.getCurrentBrewDate();
        date = dbService.getCurrentBrewDate();
        Assert.notNull(date);
    }

    @Test
    public void getAllBrews() throws Exception {
        List<BrewInfo> brews = dbService.getAllBrews();
        Assert.notEmpty(brews);
    }

    @Test
    public void createBrew() throws Exception {
        // prime cache
        try {
            dbService.getCurrentBrewDate();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        BrewInfo brew = new BrewInfo();
        brew.setName("Beer Tester");
        brew.setDescription("Testing");

        brew.setBrewDate(new Date().getTime());

        dbService.createBrew(brew);

        // test caching
        Date date = dbService.getCurrentBrewDate();
        Assert.notNull(date);
    }

    @Test
    public void createReadings() throws Exception {
        List<Recording> recordings = new ArrayList<>();
        for (TemperatureProbeRecording rec : recorder.getNextTemperatureReading()) {
            recordings.add(rec);
        }

        dbService.saveReadings(recordings);
    }

    @Test
    public void findTempReadings() throws Exception {
        Date brewDate = dbService.getMostRecentBrewDate();
        List<Recording> recordings = dbService.findTemperatureReadings(brewDate);
        recordings = dbService.findOutputReadings(brewDate);
    }



    @Autowired
    public void setDbService(DbService dbService) {
        this.dbService = dbService;
    }

    @Autowired
    public void setRecorder(TemperatureProbeMessageRecorder recorder) {
        this.recorder = recorder;
    }
}