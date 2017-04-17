package com.hopologybrewing.bcs.capture.aws.dynamo;

/**
 * Created by ddcbryanl on 1/18/17.
 */
public class DynamoDBServiceOld {
//    private static final Logger log = LoggerFactory.getLogger(DynamoDBService.class);
//
//    // figure out how to query for data in a date range (timestamp)
//
//    // create brews for a beer that have a date range and then retrieve the data for that brew
//    // super lightweight, allows you to record data over time and associate data points with a brew through simple date range mapping (between query)
//
//    // create an interface that allows brew creation/initiation and edit
//
//    public Date getCurrentBrewDate() {
//        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
//        builder.withRegion(Regions.US_EAST_1);
//        AmazonDynamoDB client = builder.build();
//
//        Date latestBrewDate = null;
//        try {
//            HashMap<String,AttributeValue> key = new HashMap<>();
//            key.put("timestamp", new AttributeValue().withN("0"));
//            key.put("id", new AttributeValue("current_brew"));
//
//            GetItemRequest request = new GetItemRequest()
//                    .withKey(key)
//                    .withTableName(DynamoConstants.BEER_FERMENTATION_TABLE);
//
//            Map<String,AttributeValue> item = client.getItem(request).getItem();
//            if (item != null) {
//                String currentBrew = item.get("brew_date").getN();
//                latestBrewDate = new Date(Long.valueOf(currentBrew));
//            } else {
//                log.error("Couldn't find the latest brew date.");
//            }
//        } catch (AmazonServiceException e) {
//            log.error("Couldn't find the latest brew date - ", e);
//        }
//
//        return latestBrewDate;
//    }
//
//    public List getAllBrews() {
//        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
//        builder.withRegion(Regions.US_EAST_1);
//        AmazonDynamoDB client = builder.build();
//
//        DynamoDB dynamoDB = new DynamoDB(client);
//        Table table = dynamoDB.getTable(DynamoConstants.BEER_FERMENTATION_TABLE);
//        Index index = table.getIndex("brew_date-type-index");
//
//        QuerySpec spec = new QuerySpec()
//
//            .withKeyConditionExpression("brew_date = :v_date and type = :v_type")
//            .withValueMap(new ValueMap()
//                    .withLong(":v_date",0L)
//                    .withString(":v_type","brew_info"));
//
//
//        ItemCollection<QueryOutcome> items = index.query(spec);
//
//        List<Recording> results = new ArrayList<>();
////        ObjectMapper mapper = new ObjectMapper();
//        for (Item item : items) {
//            log.info(item.toJSON());
//        }
//
//        return results;
//    }
//
//    public void writeRecording(BeerFermentationRecording recording) {
//        Item item = getItem(recording);
//
//        if (item != null) {
//            AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
//            builder.withRegion(Regions.US_EAST_1);
//            AmazonDynamoDB client = builder.build();
////            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.defaultClient();
//            DynamoDB dynamoDB = new DynamoDB(client);
//            Table table = dynamoDB.getTable(DynamoConstants.BEER_FERMENTATION_TABLE);
//            table.putItem(item);
//        }
//    }
//
//    private Item getItem(BeerFermentationRecording recording) {
//        Item item = null;
//
//        if (recording != null && recording.getBrewDate() != null && !StringUtils.isEmpty(recording.getType()) && !StringUtils.isEmpty(recording.getData())) {
//            item = new Item()
//                    .withKeyComponent("timestamp", recording.getTimestamp().getTime())
//                    .withKeyComponent("id", recording.getId())
//                    .with("brew_date", recording.getBrewDate().getTime())
//                    .with("type", recording.getType())
//                    .withJSON("data", recording.getData());
//        }
//
//        return item;
//    }
//
//    public List<Recording> queryRecording(Class responseObjClass, String id, long timestampLowerBound, long timestampUpperBound, int limit, boolean ascendingOrder) {
//
//        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
//        builder.withRegion(Regions.US_EAST_1);
//        AmazonDynamoDB client = builder.build();
//
//        DynamoDB dynamoDB = new DynamoDB(client);
//        Table table = dynamoDB.getTable(DynamoConstants.recordingClassTypeMap.get(responseObjClass));
//
//        QuerySpec spec = new QuerySpec()
//                .withHashKey("id", id)
//                .withScanIndexForward(ascendingOrder);
//
//        // get values in specified range
//        if (timestampLowerBound >= 0 && timestampUpperBound > 0) {
//            RangeKeyCondition rangedKeyCondition = new RangeKeyCondition("timestamp");
//            rangedKeyCondition.between(timestampLowerBound, timestampUpperBound);
//            spec.withRangeKeyCondition(rangedKeyCondition);
//        }
//
//        if (limit > 0) {
//            spec.withMaxResultSize(limit);
//        }
//
//        ItemCollection<QueryOutcome> items = table.query(spec);
//
//        List<Recording> results = new ArrayList<>();
//        ObjectMapper mapper = new ObjectMapper();
//        for (Item item : items) {
//            try {
//                results.add((Recording) mapper.readValue(item.toJSON(), DynamoConstants.tableRecordingMap.get(DynamoConstants.recordingClassTypeMap.get(responseObjClass))));
//            } catch (IOException e) {
//                log.error("Failed parsing json ", e);
//            }
//
//        }
//
//        return results;
//    }
//
//    public Recording writeRecording(Recording obj) {
//        return writeRecording(getCurrentBrewDate(), obj);
//    }
//
//    public Recording writeRecording(Date brewDate, Recording obj) {
//
//        Item item = getItem(obj);
//
//        if (item != null) {
//            AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
//            builder.withRegion(Regions.US_EAST_1);
//            AmazonDynamoDB client = builder.build();
//
//            DynamoDB dynamoDB = new DynamoDB(client);
//            Table table = dynamoDB.getTable(DynamoConstants.recordingClassTypeMap.get(obj.getClass()));
//            PutItemOutcome outcome = table.putItem(item);
//            item = outcome.getItem();
//
//            // put it in both tables for now
//            writeRecording(new BeerFermentationRecording(brewDate, obj));
//        }
//
//        return obj;
//    }
//
//    private Item getItem(Recording recording) {
//        Item item = null;
//
//        if (recording != null && !StringUtils.isEmpty(recording.getId()) && recording.getTimestamp().getTime() > 0) {
//            ObjectMapper mapper = new ObjectMapper();
//
//            try {
//                item = new Item()
//                        .withKeyComponent("id", recording.getId())
//                        .withKeyComponent("timestamp", recording.getTimestamp().getTime())
//                        .withJSON("data", mapper.writeValueAsString(recording.getData()));
//            } catch (JsonProcessingException e) {
//                log.error("Failed creating json for enabled outputs - ", e);
//            }
//        }
//
//        return item;
//    }
}
