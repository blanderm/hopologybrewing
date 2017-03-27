package com.hopologybrewing.bcs.capture.aws.dynamo;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hopologybrewing.bcs.capture.model.Recording;
import com.hopologybrewing.bcs.capture.service.DbService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ddcbryanl on 1/18/17.
 */
public class DynamoDBService implements DbService {
    private static final Logger log = LoggerFactory.getLogger(DynamoDBService.class);

    // figure out how to query for data in a date range (timestamp)

    // create brews for a beer that have a date range and then retrieve the data for that brew
    // super lightweight, allows you to record data over time and associate data points with a brew through simple date range mapping (between query)

    // create an interface that allows brew creation/initiation and edit

    public List<Recording> queryRecording(Class responseObjClass, String id, long timestampLowerBound, long timestampUpperBound, int limit, boolean ascendingOrder) {

        AmazonDynamoDBClient client = new AmazonDynamoDBClient()
                .withRegion(Regions.US_EAST_1);

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable(DynamoConstants.recordingTableMap.get(responseObjClass));

        QuerySpec spec = new QuerySpec()
                .withHashKey("id", id)
                .withScanIndexForward(ascendingOrder);

        // get values in specified range
        if (timestampLowerBound >= 0 && timestampUpperBound > 0) {
            RangeKeyCondition rangedKeyCondition = new RangeKeyCondition("timestamp");
            rangedKeyCondition.between(timestampLowerBound, timestampUpperBound);
            spec.withRangeKeyCondition(rangedKeyCondition);
        }

        if (limit > 0) {
            spec.withMaxResultSize(limit);
        }

        ItemCollection<QueryOutcome> items = table.query(spec);

        List<Recording> results = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Item item : items) {
            try {
                results.add((Recording) mapper.readValue(item.toJSON(), DynamoConstants.tableRecordingMap.get(DynamoConstants.recordingTableMap.get(responseObjClass))));
            } catch (IOException e) {
                log.error("Failed parsing json ", e);
            }

        }

        return results;
    }

    public Recording writeRecording(Recording obj) {
        Item item = getItem(obj);

        if (item != null) {
            AmazonDynamoDBClient client = new AmazonDynamoDBClient()
                    .withRegion(Regions.US_EAST_1);

            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable(DynamoConstants.recordingTableMap.get(obj.getClass()));
            PutItemOutcome outcome = table.putItem(item);
            item = outcome.getItem();
        }

        return obj;
    }

    private Item getItem(Recording recording) {
        Item item = null;

        if (recording != null && !StringUtils.isEmpty(recording.getId()) && recording.getTimestamp().getTime() > 0) {
            ObjectMapper mapper = new ObjectMapper();

            try {
                item = new Item()
                        .withKeyComponent("id", recording.getId())
                        .withKeyComponent("timestamp", recording.getTimestamp().getTime())
                        .withJSON("data", mapper.writeValueAsString(recording.getData()));
            } catch (JsonProcessingException e) {
                log.error("Failed creating json for enabled outputs - ", e);
            }
        }

        return item;
    }
}
