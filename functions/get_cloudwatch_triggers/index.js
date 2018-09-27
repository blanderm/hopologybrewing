'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

var AWS = require('aws-sdk');
var cloudwatchevents = new AWS.CloudWatchEvents();
const IOT_SNS_TOPIC_ARN = process.env['BCS_NOTIFICATION_ARN'];

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));
    cloudwatchevents.listRules({}, function(err, data) {
        if (err) console.log(err, err.stack); // an error occurred
        else {
            console.log(data);
        }

        callback(null, {
            statusCode: err ? '400' : '200',
            body: err ? JSON.stringify(err.message) : JSON.stringify(data.Rules),
            headers: {
                'Content-Type': 'application/json',
                "Access-Control-Allow-Origin" : "*", // Required for CORS support to work
                "Access-Control-Allow-Credentials" : true // Required for cookies, authorization headers with HTTPS
            }});
    })
};