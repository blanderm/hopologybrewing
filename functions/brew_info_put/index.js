'use strict';

console.log('Loading brew_info_put function');

const doc = require('dynamodb-doc');
const dynamo = new doc.DynamoDB();

exports.handler = (event, context, callback) => {
    console.log("Request received: " + JSON.stringify(event));
    var data = JSON.parse(JSON.stringify(event));
    var brewDate = (data.brewDate === undefined) ? new Date().getTime() : data.brewDate;

    var params = {
        TableName: "brew_info",
        Item: {
            brew_date: brewDate,
            description: data.description,
            last_updated: new Date().getTime(),
            name: data.name
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
        var respMsg = err ? err.message : "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr;
        console.log(respMsg);
        callback(null, respMsg);
    });
};
