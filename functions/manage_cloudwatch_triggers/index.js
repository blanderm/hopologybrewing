'use strict';

console.log('Loading manage_cloudwatch_triggers function');

var AWS = require('aws-sdk');

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));
    callback(null, "Completed");
};
