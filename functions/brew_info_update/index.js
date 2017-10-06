'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

const AWS = require('aws-sdk');
const sns = new AWS.SNS();
const dynamo = new AWS.DynamoDB.DocumentClient();
const CLICK_SINGLE = "SINGLE";
const CLICK_DOUBLE = "DOUBLE";
const CLICK_LONG = "LONG";
const BREW_INFO_TABLE_NAME = "brew_info";
const IOT_SNS_TOPIC_ARN = process.env['IOT_BUTTON_ARN'];

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));

    var brewDate = 0;
    dynamo.scan({TableName: BREW_INFO_TABLE_NAME}, function (err, resp) {
        var response = err ? err.message : JSON.parse(JSON.stringify(resp));
        console.log("Raw response from scan: " + JSON.stringify(response));

        var now = new Date().getTime();
        var item;
        for (var i = 0; i < response.Items.length; i++) {
            item = response.Items[i];
            if ((item.brew_complete_date === undefined && now >= item.brew_date) || (now >= item.brew_date && now <= item.brew_complete_date)) {
                brewDate = item.brew_date;
                console.log("Brew date: " + item.brew_date);
                processEvent(event, callback, brewDate);
                break;
            }
        }
    });
};

function processEvent(event, callback, brewDate) {
    let params = {
        Key: {
            brew_date: brewDate
        },
        TableName: BREW_INFO_TABLE_NAME,
        UpdateExpression: "set last_updated = :lu, #an=:av",
    };

    let clickType = (event.clickType ? event.clickType : JSON.parse(event.body).clickType);

    if (CLICK_SINGLE == clickType) {
        console.log("Received event type " + clickType + ", inserting timestamp for yeast pitch.");
        updateBrewInfo(params, "yeast_pitch", callback);
    } else if (CLICK_DOUBLE == clickType) {
        console.log("Received event type " + clickType + ", inserting timestamp for crash start.");
        updateBrewInfo(params, "crash_start", callback);
    } else if (CLICK_LONG == clickType) {
        console.log("Received event type " + clickType + ", inserting timestamp for brew completion date.");
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
        var respMsg = err ? err.message : "Updated attribute " + attrName + " with value " + now;
        console.log(respMsg);
        sendNotifiction(respMsg, callback, err);
    });
}

function sendNotifiction(msg, callback, err) {
    sns.publish({
        Message: msg,
        TopicArn: IOT_SNS_TOPIC_ARN
    }, function(err, data) {
        if (err) console.log(err, err.stack);
        else console.log('Sent confirmation message: ' + msg);
        callback(null, {
            statusCode: err ? '400' : '200',
            body: err ? JSON.stringify(err.message) : JSON.stringify({ message: msg}),
            headers: {
                'Content-Type': 'application/json',
                "Access-Control-Allow-Origin" : "*", // Required for CORS support to work
                "Access-Control-Allow-Credentials" : true // Required for cookies, authorization headers with HTTPS
            }});
    });
}