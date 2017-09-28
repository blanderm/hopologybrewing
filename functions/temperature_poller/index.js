'use strict';

console.log('Loading temperature_poller function');

const doc = require('dynamodb-doc');
const dynamo = new doc.DynamoDB();
const http = require('http');

const AWS = require('aws-sdk');

const encryptedHost = process.env['BCS_IP'];
const encryptedUser = process.env['USER'];
const encryptedPassword = process.env['PWD'];
let decryptedHost;
let decryptedUser;
let decryptedPwd;


exports.handler = (event, context, callback) => {
    if (decryptedHost && decryptedUser && decryptedPwd) {
        getActiveBrew(event, context, callback);
    } else {
        // Decrypt code should run once and variables stored outside of the function
        // handler so that these are decrypted once per container
        const kms = new AWS.KMS();
        kms.decrypt({CiphertextBlob: new Buffer(encryptedHost, 'base64')}, (err, data) => {
            if (err) {
                console.log('Decrypt error:', err);
                return callback(err);
            }

            decryptedHost = data.Plaintext.toString('ascii');
            kms.decrypt({CiphertextBlob: new Buffer(encryptedUser, 'base64')}, (err, data) => {
                if (err) {
                    console.log('Decrypt error:', err);
                    return callback(err);
                }

                decryptedUser = data.Plaintext.toString('ascii');
                kms.decrypt({CiphertextBlob: new Buffer(encryptedPassword, 'base64')}, (err, data) => {
                    if (err) {
                        console.log('Decrypt error:', err);
                        return callback(err);
                    }
                    decryptedPwd = data.Plaintext.toString('ascii');
                    getActiveBrew(event, context, callback);
                });
            });
        });
    }
};

function getActiveBrew(event, context, callback) {
    var brewDate = 0;
    dynamo.scan({TableName: "brew_info"}, function (err, resp) {
        var response = err ? err.message : JSON.parse(JSON.stringify(resp));
        console.log("Raw response from scan: " + JSON.stringify(response));

        var now = new Date().getTime();
        var item;
        for (var i = 0; i < response.Items.length; i++) {
            item = response.Items[i];
            if ((item.brew_complete_date === undefined && now >= item.yeastPitch) || (now >= item.yeastPitch && now <= item.brew_complete_date)) {
                brewDate = item.brew_date;
                console.log("Brew date: " + item.brew_date);
                processEvent(event, context, callback, brewDate);
                break;
            }
        }
    });
}

function processEvent(event, context, callback, brewDate) {
    var results = [];
    var size = 2;
    for (var i = 0; i < size; i++) {
        var request = http.request({
                host: decryptedHost,
                auth: decryptedUser + ':' + decryptedPwd,
                path: '/api/temp/' + i,
                port: '80',
                method: 'GET'
            },
            function (response) {
                console.log('STATUS: ' + response.statusCode);
                console.log('HEADERS: ' + JSON.stringify(response.headers));
                response.setEncoding('utf8');
                var rawData = '';
                response.on('data', function (chunk) {
                    rawData += chunk;
                    console.log("data: " + rawData);
                });

                response.on('end', () => {
                    var parsedData = JSON.parse(rawData);
                    results.push(parsedData);
                    console.log("Data received from controller: " + JSON.stringify(parsedData));
                    console.log("Results: " + JSON.stringify(results));
                    recordReadings(results, size, brewDate);
                });
            });

        request.end();
    }

    callback(null, "Completed temp reading capture.");
}

function recordReadings(results, size, brewDate) {
    if (results.length === size) {
        var data = JSON.parse("{}");
        results.forEach(function(item) {
            data[item.name] = item;
        });

        var params = {
            TableName: "brew_recordings",
            Item: {
                type: "temperature_recording",
                timestamp: new Date().getTime(),
                brew_date: brewDate,
                data: data
            }
        };

        var paramsStr = JSON.stringify(params);
        console.log("Params to persist: " + paramsStr);
        dynamo.putItem(params, function (err, resp) {
            var respMsg = err ? err.message : "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr;
            console.log(respMsg);
        });
    }
}