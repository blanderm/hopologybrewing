'use strict';

console.log('Loading brew_info_update function');

var AWS = require('aws-sdk');
var dynamo = new AWS.DynamoDB.DocumentClient();
const CLICK_SINGLE = "SINGLE";
const CLICK_DOUBLE = "DOUBLE";
const CLICK_LONG = "LONG";

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));
    let data = JSON.parse(JSON.stringify(event));

    var brewDate = 0;
    dynamo.scan({TableName: "brew_info"}, function (err, resp) {
        var response = err ? err.message : JSON.parse(JSON.stringify(resp));
        console.log("Raw response from scan: " + JSON.stringify(response));

        var now = new Date().getTime();
        var item;
        for (var i = 0; i < response.Items.length; i++) {
            item = response.Items[i];
            if ((item.brew_complete_date === undefined && now >= item.brew_date) || (now >= item.brew_date && now <= item.brew_complete_date)) {
                brewDate = item.brew_date;
                console.log("Brew date: " + item.brew_date);
                processEvent(event, context, callback, brewDate);
                break;
            }
        }
    });
};

function processEvent(event, context, callback, brewDate) {
    let params = {
        Key: {
            brew_date: brewDate
        },
        TableName: "brew_info",
        UpdateExpression: "set last_updated = :lu, #an=:av",
    };

    if (CLICK_SINGLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", inserting timestamp for yeast pitch.");
        updateBrewInfo(params, "yeast_pitch", callback);
    } else if (CLICK_DOUBLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", inserting timestamp for crash start.");
        updateBrewInfo(params, "crash_start", callback);
    } else if (CLICK_LONG == event.clickType) {
        console.log("Received event type " + event.clickType + ", inserting timestamp for brew completion date.");
        updateBrewInfo(params, "brew_completion_date", callback);
    } else {
        console.log("Received event without click type, ignoring.");
    }
}

function updateBrewInfo(params, attrName, callback) {
    let now = new Date().getTime();
    params.ExpressionAttributeNames = {
        "#an": attrName
    };

    params.ExpressionAttributeValues = {
        ":lu": now,
        ":av": now,
    };

    var paramsStr = JSON.stringify(params);
    console.log("Params to persist: " + paramsStr);
    dynamo.update(params, function (err, resp) {
        var respMsg = err ? err.message : "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr;
        console.log(respMsg);
        callback(null, respMsg);
    });
}