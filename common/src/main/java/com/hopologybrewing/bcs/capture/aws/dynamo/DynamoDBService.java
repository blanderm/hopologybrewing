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

import java.io.UTFDataFormatException;
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
    private static final String BD_ALL_CACHE_KEY = "bd_all_cache_key";
    private static final String BD_CURR_CACHE_KEY = "bd_current_cache_key";
    private static final String BD_RECENT_CACHE_KEY = "bd_recent_cache_key";
    private static final Cache<String, Date> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(30L, TimeUnit.DAYS)
            .build();
    private static final Cache<String, List<BrewInfo>> brewsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30L, TimeUnit.DAYS)
            .build();

    // todo: create an interface that allows brew creation/initiation and edit

    @Override
    public Date getCurrentBrewDate() throws ExecutionException {
        return cache.get(BD_CURR_CACHE_KEY, new CurrentBrewDateCallable());
    }

    @Override
    public Date getMostRecentBrewDate() throws ExecutionException {
        return cache.get(BD_RECENT_CACHE_KEY, new MostRecentBrewDateCallable());
    }

    @Override
    public List<BrewInfo> getAllBrews() throws ExecutionException {
        return brewsCache.get(BD_ALL_CACHE_KEY, new AllBrewsCallable());
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
        brewsCache.put(BD_ALL_CACHE_KEY, new AllBrewsCallable().call());

        return newBrew;
    }

    @Override
    public List<Recording> findOutputReadings(Date date, long lowerRange, long upperRange) {
        log.info("findOutputReadings : querying DyanmoDB for brew date " + date + " and range " + new Date(lowerRange) + " to " + new Date(upperRange));
        Date brewDate;
        Date timestamp;
        Output output;
        OutputRecording recording;
        List<Recording> recordings = new LinkedList<>();
        for (Item item : findReadings(date, OutputRecording.OUTPUT_TYPE, lowerRange, upperRange)) {
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
    public List<Recording> findTemperatureReadings(Date date, long lowerRange, long upperRange) {
        log.info("findTemperatureReadings : querying DyanmoDB for brew date " + date + " and range " + new Date(lowerRange) + " to " + new Date(upperRange));
        Date brewDate;
        Date timestamp;
        TemperatureProbe probe;
        TemperatureProbeRecording recording;
        List<Recording> recordings = new LinkedList<>();
        for (Item item : findReadings(date, TemperatureProbeRecording.TEMPERATURE_TYPE, lowerRange, upperRange)) {
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
//                            case "resistance":
//                                probe.setResistance(((BigDecimal) entry.getValue()).doubleValue());
//                                break;
                            case "enabled":
                                if (entry.getValue() instanceof String) {
                                    probe.setEnabled(Boolean.parseBoolean((String) entry.getValue()));
                                } else {
                                    probe.setEnabled((boolean) entry.getValue());
                                }
                                break;
//                            case "coefficients":
//                                for (BigDecimal coeff : (List<BigDecimal>) entry.getValue()) {
//                                    probe.addCoefficient(coeff.doubleValue());
//                                }
//                                break;
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

    private ItemCollection<QueryOutcome> findReadings(Date date, String type, long lowerRange, long upperRange) {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.withRegion(CURRENT_REGION);
        DynamoDB dynamoDB = new DynamoDB(builder.build());
        Table table = dynamoDB.getTable(DynamoConstants.BREW_READINGS_TABLE);

        QuerySpec spec = new QuerySpec()
                .withKeyConditionExpression("#ts between :v_dateLower and :v_dateUpper and #t = :v_type")
                .withFilterExpression("brewDate = :v_bDate")
                .withNameMap(new NameMap()
                        .with("#ts", "timestamp")
                        .with("#t", "type"))
                .withValueMap(new ValueMap()
                        .withNumber(":v_dateLower", lowerRange)
                        .withNumber(":v_dateUpper", upperRange)
                        .withString(":v_type", type)
                        .withNumber(":v_bDate", date.getTime()))
                .withConsistentRead(false);

        return table.query(spec);
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
                log.debug("Couldn't find the latest brew date.  It's possible that there isn't a brew fermenting right now.");
                throw new UTFDataFormatException("Couldn't find the latest brew date.  It's possible that there isn't a brew fermenting right now.");
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
                log.debug("Couldn't find the most recent brew date.  It's possible that there isn't any brew info in the db.");
                throw new UTFDataFormatException("Couldn't find the latest brew date.  It's possible that there isn't any brew info in the db.");
            }

            return new Date(latestBrewDate);
        }
    }

    private class AllBrewsCallable implements Callable<List<BrewInfo>> {
        @Override
        public List<BrewInfo> call() throws Exception {
            AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
            builder.withRegion(CURRENT_REGION);

            DynamoDBMapper mapper = new DynamoDBMapper(builder.build());
            return mapper.scan(BrewInfo.class, new DynamoDBScanExpression());
        }
    }
}
