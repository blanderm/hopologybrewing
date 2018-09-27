'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

const doc = require('dynamodb-doc');
const dynamo = new doc.DynamoDB();
const http = require('http');

const AWS = require('aws-sdk');

const encryptedHost = process.env['BCS_IP'];
const encryptedUser = process.env['USER'];
const encryptedPassword = process.env['PWD'];
const IOT_SNS_TOPIC_ARN = process.env['BCS_NOTIFICATION_ARN'];
const SOCKET_TIMEOUT = 400;
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
    let brewDate = 0;
    dynamo.scan({TableName: "brew_info"}, function (err, resp) {
        let response = err ? err.message : JSON.parse(JSON.stringify(resp));
        console.log("Raw response from scan: " + JSON.stringify(response));

        let now = new Date().getTime();
        let item;
        for (let i = 0; i < response.Items.length; i++) {
            item = response.Items[i];
            if ((item.crash_start === undefined && now >= item.yeast_pitch) || (now >= item.yeast_pitch && now <= item.crash_start)) {
                brewDate = item.brew_date;
                console.log("Brew date: " + item.brew_date + " Yeast Pitch: " + item.yeast_pitch);
                break;
            }
        }

        if (brewDate) {
            processEvent(event, context, callback, brewDate);
        } else {
            sendNotification("Failed to find active brew.  You should disable the pollers to avoid polling charges or create an active brew.", callback);
        }
    });
}

function processEvent(event, context, callback, brewDate) {
    let results = [];
    let size = 2;
    for (let i = 0; i < size; i++) {
        let request = http.request({
                host: decryptedHost,
                auth: decryptedUser + ':' + decryptedPwd,
                path: '/api/output/' + i,
                port: '8888',
                method: 'GET',
                timeout: SOCKET_TIMEOUT
            },
            function (response) {
                console.log('STATUS: ' + response.statusCode);
                console.log('HEADERS: ' + JSON.stringify(response.headers));
                response.setEncoding('utf8');
                let rawData = '';
                response.on('data', function (chunk) {
                    rawData += chunk;
                });

                response.on('end', () => {
                    let parsedData = JSON.parse(rawData);
                    results.push(parsedData);
                    console.log("Results: " + JSON.stringify(results));
                    recordReadings(results, size, brewDate);
                });
            }).on("error", (err) => {
                console.log("Error: " + err.message);
                sendNotification("Output Poller :: Error: " + err.message, callback);
            }).on('timeout', () => {
                console.log("Request timed out obtaining BCS data.");
                request.abort();
                sendNotification("Output Poller :: Request timed out obtaining BCS data.", callback);
            });

        request.end();
    }
}

function recordReadings(results, size, brewDate, callback) {
    if (results.length === size) {
        let data = JSON.parse("{}");
        let atLeastOneOn = false;
        results.forEach(function (item) {
            data[item.name] = item;

            if (!atLeastOneOn) {
                atLeastOneOn = item.on;
            }
        });

        // only record if at least one pump was on
        if (atLeastOneOn) {
            let params = {
                TableName: "brew_recordings",
                Item: {
                    type: "output_recording",
                    timestamp: new Date().getTime(),
                    brew_date: brewDate,
                    data: data
                }
            };

            let paramsStr = JSON.stringify(params);
            console.log("Params to persist: " + paramsStr);
            dynamo.putItem(params, function (err, resp) {
                let respMsg;
                if (err) {
                    respMsg = err.message;
                    sendNotification("Output Poller :: " + err.message, callback);
                } else {
                    respMsg = "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr;
                }

                console.log(respMsg);

                if (callback && !err) callback(null, "Completed output reading capture.");
            });
        }
    }
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