'use strict';

console.log('Loading brew_info_put function');

const doc = require('dynamodb-doc');
const dynamo = new doc.DynamoDB();

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));
    var data = JSON.parse(event.body);
    console.log("Body received: " + JSON.stringify(data));
    var brewDate = (data.month !== undefined && data.day !== undefined && data.year !== undefined) ? new Date(data.year, data.month, data.day) : new Date();
    brewDate.setUTCHours(4);
    brewDate.setUTCMinutes(0);
    brewDate.setUTCSeconds(0);
    brewDate.setUTCMilliseconds(0);

    var params = {
        TableName: "brew_info",
        Item: {
            brew_date: brewDate.getTime(),
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

    var paramsStr = JSON.stringify(params);
    console.log("Params to persist: " + paramsStr);
    dynamo.putItem(params, function (err, resp) {
        console.log(err ? err.message : "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr);
        callback(null, {
            statusCode: err ? '400' : '200',
            body: err ? err.message : JSON.stringify({ message: 'Successfully persisted item', item: params.Item}),
            headers: {
                'Content-Type': 'application/json',
                "Access-Control-Allow-Origin" : "*", // Required for CORS support to work
                "Access-Control-Allow-Credentials" : true // Required for cookies, authorization headers with HTTPS
            }});
    });
};
