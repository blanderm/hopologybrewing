'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

const doc = require('dynamodb-doc');
const dynamo = new doc.DynamoDB();
const BREW_INFO_TABLE_NAME = "brew_info";

exports.handler = (event, context, callback) => {
    let data = JSON.parse(event.body);
    let brewDateMs = (data.brewDate ? data.brewDate : (data.month !== undefined && data.day !== undefined && data.year !== undefined) ? new Date(data.year, data.month - 1, data.day).getTime() : new Date().getTime());

    console.log(brewDateMs);

    let params = {
        TableName: BREW_INFO_TABLE_NAME,
        Item: {
            brew_date: brewDateMs,
            name: data.name,
            description: data.description,
            last_updated: new Date().getTime()
        }
    };

    if (data.additionalAttributes != undefined) {
        data.additionalAttributes.forEach(function (attr) {
            params.Item[attr.name] = attr.value;
        });
    }

    let paramsStr = JSON.stringify(params);
    console.log("Params to persist: " + paramsStr);
    dynamo.putItem(params, function (err, resp) {
        console.log(err ? err.message : "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr);
        callback(null, {
            statusCode: err ? '400' : '200',
            body: err ? JSON.stringify(err.message) : JSON.stringify({ message: 'Successfully persisted item', item: params.Item}),
            headers: {
                'Content-Type': 'application/json',
                "Access-Control-Allow-Origin" : "*", // Required for CORS support to work
                "Access-Control-Allow-Credentials" : true // Required for cookies, authorization headers with HTTPS
            }});
    });
};
