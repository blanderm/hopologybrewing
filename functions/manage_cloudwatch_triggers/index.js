'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

var AWS = require('aws-sdk');
var cloudwatchevents = new AWS.CloudWatchEvents();
const CLICK_SINGLE = "SINGLE";
const CLICK_DOUBLE = "DOUBLE";
const CLICK_LONG = "LONG";
const IOT_SNS_TOPIC_ARN = process.env['IOT_BUTTON_ARN'];


exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));

    let clickType = (event.clickType ? event.clickType : JSON.parse(event.body).clickType);

    if (CLICK_SINGLE == clickType) {
        console.log("Received event type " + clickType + ", enabling pollers");
        updateRule("every-minute", true, callback, false);
        updateRule("every-five-minutes", true, callback, true);
    } else if (CLICK_DOUBLE == clickType) {
        console.log("Received event type " + clickType + ", disabling pollers");
        updateRule("every-minute", false, callback, false);
        updateRule("every-five-minutes", false, callback, true);
    } else if (CLICK_LONG == clickType) {
        console.log("Received event type " + clickType + ", disabling output poller");
        updateRule("every-minute", false, callback, true);
    } else {
        console.log("Received event but ignoring.");
        sendNotification("Received event but ignoring.", callback);
    }
};

function updateRule(ruleName, enable, callback, notify) {
    var params = {
        Name: ruleName
    };

    if (enable) {
        cloudwatchevents.enableRule(params, function (err, data) {
            handleRuleResult(err, "enabled", ruleName, callback, notify);
        });
    } else {
        cloudwatchevents.disableRule(params, function (err, data) {
            handleRuleResult(err, "disabled", ruleName, callback, notify);
        });
    }
}

function handleRuleResult(err, type, ruleName, callback, notify) {
    if (err) {
        console.log(err.message, err.stack);
        sendNotification(err.message, callback, err);
    } else {
        console.log("Successfully " + type + " rule: " + ruleName);
    }

    if(notify) sendNotification("Successfully " + type + " rules", callback);
}

function sendNotification(msg, callback, err) {
    let sns = new AWS.SNS();

    sns.publish({
        Message: msg,
        TopicArn: IOT_SNS_TOPIC_ARN
    }, function(snsErr, data) {
        if (snsErr) console.log(snsErr, snsErr.stack);
        else console.log('Sent confirmation message: ' + msg);

        if (callback) callback(null, {
            statusCode: err ? '400' : '200',
            body: err ? JSON.stringify(err.message) : JSON.stringify({ message: msg}),
            headers: {
                'Content-Type': 'application/json',
                "Access-Control-Allow-Origin" : "*", // Required for CORS support to work
                "Access-Control-Allow-Credentials" : true // Required for cookies, authorization headers with HTTPS
            }});
    });
}