package com.hopologybrewing.bcs.capture.service;

import com.hopologybrewing.bcs.capture.batch.DataLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration("classpath:applicationContext.xml")
@EnableAutoConfiguration
public class DataLoaderTest {
    private DataLoader dataLoader;

    @Test
    public void loadData() throws Exception {
        dataLoader.loadData();
    }

    @Autowired
    public void setDataLoader(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }
}