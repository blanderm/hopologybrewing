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

    if (CLICK_SINGLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", enabling pollers");
        updateRule("every-minute", true, callback, false);
        updateRule("every-five-minutes", true, callback, true);
    } else if (CLICK_DOUBLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", disabling pollers");
        updateRule("every-minute", false, callback, false);
        updateRule("every-five-minutes", false, callback, true);
    } else if (CLICK_LONG == event.clickType) {
        console.log("Received event type " + event.clickType + ", disabling output poller");
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
        sendNotification(err.message, callback);
    } else {
        console.log("Successfully " + type + " rule: " + ruleName);
    }

    if(notify) sendNotification("Successfully " + type + " rules", callback);
}

function sendNotification(msg, callback) {
    let sns = new AWS.SNS();

    sns.publish({
        Message: msg,
        TopicArn: IOT_SNS_TOPIC_ARN
    }, function(err, data) {
        if (err) console.log(err, err.stack);
        else console.log('Sent confirmation message: ' + msg);

        if (callback) callback(null, msg);
    });
}