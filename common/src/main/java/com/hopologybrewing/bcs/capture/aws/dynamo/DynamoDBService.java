package com.hopologybrewing.bcs.capture.aws.dynamo;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.utils.NameMap;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hopologybrewing.bcs.capture.model.*;
import com.hopologybrewing.bcs.capture.service.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by ddcbryanl on 1/18/17.
 */
public class DynamoDBService implements DbService {
    private static final Logger log = LoggerFactory.getLogger(DynamoDBService.class);
    private static final Regions CURRENT_REGION = Regions.US_EAST_1;
    private static final String BD_CURR_CACHE_KEY = "bd_current_cache_key";
    private static final String BD_RECENT_CACHE_KEY = "bd_recent_cache_key";
    private static final Cache<String, Date> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(24L, TimeUnit.HOURS)
            .build();

    // figure out how to query for data in a date range (timestamp)

    // create brews for a beer that have a date range and then retrieve the data for that brew
    // super lightweight, allows you to record data over time and associate data points with a brew through simple date range mapping (between query)

    // create an interface that allows brew creation/initiation and edit

    @Override
    public Date getCurrentBrewDate() throws ExecutionException {
        return cache.get(BD_CURR_CACHE_KEY, new CurrentBrewDateCallable());
    }

    @Override
    public Date getMostRecentBrewDate() throws ExecutionException {
        return cache.get(BD_RECENT_CACHE_KEY, new MostRecentBrewDateCallable());
    }

    @Override
    public BrewInfo createBrew(BrewInfo newBrew) throws Exception {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.withRegion(CURRENT_REGION);

        DynamoDBMapper mapper = new DynamoDBMapper(builder.build());
        newBrew.setLastUpdated(new Date());
        mapper.save(newBrew);

        // clear the currentBrewCache
        cache.put(BD_CURR_CACHE_KEY, new CurrentBrewDateCallable().call());
        cache.put(BD_RECENT_CACHE_KEY, new MostRecentBrewDateCallable().call());

        return newBrew;
    }

    @Override
    public List<BrewInfo> getAllBrews() {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.withRegion(CURRENT_REGION);

        DynamoDBMapper mapper = new DynamoDBMapper(builder.build());
        return mapper.scan(BrewInfo.class, new DynamoDBScanExpression());
    }

    @Override
    public List<Recording> findOutputReadings(Date date) {
        log.info("findOutputReadings : querying DyanmoDB for date " + date);
        Date brewDate;
        Date timestamp;
        Output output;
        OutputRecording recording;
        List<Recording> recordings = new ArrayList<>();
        for (Item item : findReadings(date, OutputRecording.OUTPUT_TYPE)) {
            timestamp = new Date(item.getLong("timestamp"));
            brewDate = new Date(item.getLong("brewDate"));

            for (Object o : ((Map) item.get("data")).values()) {
                if (o instanceof Map) {
                    recording = new OutputRecording();
                    recording.setTimestamp(timestamp);
                    recording.setBrewDate(brewDate);
                    output = new Output();

                    for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) ((Map) o).entrySet()) {
                        switch (entry.getKey()) {
                            case "name":
                                output.setName((String) entry.getValue());
                                break;
                            case "enabled":
                                if (entry.getValue() instanceof String) {
                                    output.setEnabled(Boolean.parseBoolean((String) entry.getValue()));
                                } else {
                                    output.setEnabled((boolean) entry.getValue());
                                }
                                break;
                            case "on":
                                if (entry.getValue() instanceof String) {
                                    output.setOn(Boolean.parseBoolean((String) entry.getValue()));
                                } else {
                                    output.setOn((boolean) entry.getValue());
                                }
                                break;
                        }
                    }

                    recording.setData(output);
                    recordings.add(recording);
                }
            }
        }

        log.info("findOutputReadings : found " + recordings.size() + " recordings");
        return recordings;
    }

    @Override
    public List<Recording> findTemperatureReadings(Date date) {
        log.info("findTemperatureReadings : querying DyanmoDB for date " + date);
        Date brewDate;
        Date timestamp;
        TemperatureProbe probe;
        TemperatureProbeRecording recording;
        List<Recording> recordings = new ArrayList<>();
        for (Item item : findReadings(date, TemperatureProbeRecording.TEMPERATURE_TYPE)) {
            timestamp = new Date(item.getLong("timestamp"));
            brewDate = new Date(item.getLong("brewDate"));

            // todo: there has to be a better way to do this...consider DBMapper.
            for (Object o : ((Map) item.get("data")).values()) {
                if (o instanceof Map) {
                    recording = new TemperatureProbeRecording();
                    recording.setTimestamp(timestamp);
                    recording.setBrewDate(brewDate);
                    probe = new TemperatureProbe();

                    for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>) ((Map) o).entrySet()) {
                        switch (entry.getKey()) {
                            case "name":
                                probe.setName((String) entry.getValue());
                                break;
                            case "temp":
                                probe.setTemp(((BigDecimal) entry.getValue()).doubleValue());
                                break;
                            case "setpoint":
                                probe.setSetpoint(((BigDecimal) entry.getValue()).doubleValue());
                                break;
                            case "resistance":
                                probe.setResistance(((BigDecimal) entry.getValue()).doubleValue());
                                break;
                            case "enabled":
                                if (entry.getValue() instanceof String) {
                                    probe.setEnabled(Boolean.parseBoolean((String) entry.getValue()));
                                } else {
                                    probe.setEnabled((boolean) entry.getValue());
                                }
                                break;
                            case "coefficients":
                                for (BigDecimal coeff : (List<BigDecimal>) entry.getValue()) {
                                    probe.addCoefficient(coeff.doubleValue());
                                }
                                break;
                        }
                    }

                    recording.setData(probe);
                    recordings.add(recording);
                }
            }
        }

        log.info("findTemperatureReadings : found " + recordings.size() + " recordings");
        return recordings;
    }

    private ItemCollection<QueryOutcome> findReadings(Date date, String type) {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.withRegion(CURRENT_REGION);
        DynamoDB dynamoDB = new DynamoDB(builder.build());
        Index index = dynamoDB.getTable(DynamoConstants.BREW_READINGS_TABLE).getIndex("brewDate-type-index");

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("brewDate = :v_date and #t = :v_type")
                .withNameMap(new NameMap()
                        .with("#t", "type"))
                .withValueMap(new ValueMap()
                        .withNumber(":v_date", date.getTime())
                        .withString(":v_type", type));

        return index.query(spec);
    }

    @Override
    public void saveReadings(List<Recording> message) {
        // returns null if item creation fails
        Item item = convertToItem(message);

        // todo: revisit, for now drop data if a failure occurs
        if (item != null) {
            AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
            builder.withRegion(CURRENT_REGION);
            AmazonDynamoDB client = builder.build();
            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable(DynamoConstants.BREW_READINGS_TABLE);
            table.putItem(item);
        }
    }

    private Item convertToItem(List<Recording> message) {
        Item item = null;

        try {
            String type = null;
            Date brewDate = null;
            Date timestamp = null;
            Map<String, Object> recordingMap = new HashMap<>();
            for (Recording recording : message) {
                recordingMap.put(recording.getName(), recording.getData());

                // data should be the same so overwriting isn't a concern
                timestamp = recording.getTimestamp();

                if (recording.getBrewDate() == null) {
                    brewDate = getCurrentBrewDate();
                } else {
                    brewDate = recording.getBrewDate();
                }

                type = recording.getType();
            }

            // populate item
            ObjectMapper mapper = new ObjectMapper();
            item = new Item()
                    .withKeyComponent("timestamp", timestamp.getTime())
                    .withKeyComponent("type", type)
                    .with("brewDate", brewDate.getTime())
                    .withJSON("data", mapper.writeValueAsString(recordingMap));
        } catch (ExecutionException e) {
            log.error("Failed creating item - ", e);
        } catch (JsonProcessingException e) {
            log.error("Failed creating item - ", e);
        }

        return item;
    }

    private class CurrentBrewDateCallable implements Callable<Date> {
        @Override
        public Date call() throws Exception {
            Date latestBrewDate = null;
            List<BrewInfo> list = getAllBrews();

            for (BrewInfo info : list) {
                if (info.isCurrentBrew()) {
                    latestBrewDate = info.getBrewDateAsDate();
                    break;
                }
            }

            if (latestBrewDate == null) {
                log.error("Couldn't find the latest brew date.  It's possible that there isn't a brew fermenting right now.");
            }

            return latestBrewDate;
        }
    }

    private class MostRecentBrewDateCallable implements Callable<Date> {
        @Override
        public Date call() throws Exception {
            long latestBrewDate = 0L;

            List<BrewInfo> list = getAllBrews();
            int index = BrewInfo.getMostRecentBrewIndex(list);

            if (index >= 0) {
                latestBrewDate = list.get(index).getBrewDate();
            } else {
                log.error("Couldn't find the latest brew date.  It's possible that there isn't any brew info in the db.");
            }

            return new Date(latestBrewDate);
        }
    }
}
