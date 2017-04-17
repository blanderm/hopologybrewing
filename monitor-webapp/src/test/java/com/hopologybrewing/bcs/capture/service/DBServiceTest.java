package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.aws.dynamo.DynamoDBService;
import com.hopologybrewing.bcs.capture.batch.TemperatureProbeMessageRecorder;
import com.hopologybrewing.bcs.capture.model.BrewInfo;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.model.TemperatureProbeRecording;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
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
        dbService.getCurrentBrewDate();

        BrewInfo brew = new BrewInfo();
        brew.setName("NE IPA 1 - Mosaic");
        brew.setDescription("First transition to a more wheat based IPA with a 4.0 SRM");

        brew.setBrewDate(1491177600000L);

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
        Date brewDate = dbService.getCurrentBrewDate();
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