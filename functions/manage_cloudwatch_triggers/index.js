'use strict';

console.log('Loading manage_cloudwatch_triggers function');

var AWS = require('aws-sdk');
var cloudwatchevents = new AWS.CloudWatchEvents();
const CLICK_SINGLE = "SINGLE";
const CLICK_DOUBLE = "DOUBLE";
const CLICK_LONG = "LONG";
const IOT_SNS_TOPIC_ARN = process.env['IOT_BUTTON_ARN'];


exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));

    let myCallback = function(msg) {
        let sns = new AWS.SNS();

        sns.publish({
            Message: msg,
            TopicArn: IOT_SNS_TOPIC_ARN
        }, function(err, data) {
            if (err) console.log(err, err.stack);
            else console.log('Sent confirmation message: ' + msg);
            callback(null, null);
        });
    };

    let msg = '';

    if (CLICK_SINGLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", enabling pollers");
        msg = updateRule("every-minute", true, null, null);
        updateRule("every-five-minutes", true, msg, myCallback);
    } else if (CLICK_DOUBLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", disabling pollers");
        msg = updateRule("every-minute", false, null, null);
        updateRule("every-five-minutes", false, msg, myCallback);
    } else if (CLICK_LONG == event.clickType) {
            console.log("Received event type " + event.clickType + ", disabling output poller");
            updateRule("every-minute", false, null, myCallback);
    } else {
        console.log("Received event but ignoring.");
        myCallback("Received event but ignoring.");
    }
};

function updateRule(ruleName, enable, msg, callback) {
    var params = {
        Name: ruleName
    };

    if (enable) {
        cloudwatchevents.enableRule(params, function (err, data) {
            handleRuleResult(err, data, "enabled", ruleName, msg, callback);
        });
    } else {
        cloudwatchevents.disableRule(params, function (err, data) {
            handleRuleResult(err, data, "disabled", ruleName, msg, callback);
        });
    }
}

function handleRuleResult(err, data, type, ruleName, msg, callback) {
    let respMsg = err ? err.message : "Successfully " + type + " rule: " + ruleName;
    if (err) console.log(respMsg, err.stack);
    else console.log(respMsg);

    msg = msg == null ? respMsg : msg += " // " + respMsg;

    if(callback != null) callback(msg);
    else return msg;
}