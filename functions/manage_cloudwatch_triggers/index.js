'use strict';

console.log('Loading manage_cloudwatch_triggers function');

var AWS = require('aws-sdk');
var cloudwatchevents = new AWS.CloudWatchEvents();
const CLICK_SINGLE = "SINGLE";
const CLICK_DOUBLE = "DOUBLE";
const CLICK_LONG = "LONG"; // unused


exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));

    if (CLICK_SINGLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", enabling pollers");
        updateRule("every-minute", true);
        updateRule("every-five-minutes", true, callback);
    } else if (CLICK_DOUBLE == event.clickType) {
        console.log("Received event type " + event.clickType + ", disabling pollers");
        updateRule("every-minute", false);
        updateRule("every-five-minutes", false, callback);
    } else {
        console.log("Received event but ignoring.");
        callback(null, null);
    }
};

function updateRule(ruleName, enable, callback) {
    var params = {
        Name: ruleName
    };

    if (enable) {
        cloudwatchevents.enableRule(params, function (err, data) {
            if (err) console.log(err, err.stack);
            else console.log("Successfully enabled rule: " + params.Name);
            if(callback) callback(null, null);
        });
    } else {
        cloudwatchevents.disableRule(params, function (err, data) {
            if (err) console.log(err, err.stack);
            else console.log("Successfully enabled rule: " + params.Name);
            if(callback) callback(null, null);
        });
    }
}