'use strict';

console.log('Loading ' + process.env['LAMBDA_FUNCTION_NAME'] + ' function');

const http = require('http');
const AWS = require('aws-sdk');
AWS.config.logger = console;
const dynamo = new AWS.DynamoDB();
const ssm = new AWS.SSM();

const IOT_SNS_TOPIC_ARN = process.env['BCS_NOTIFICATION_ARN'];
const SOCKET_TIMEOUT = 2000;
var decryptedHost;
var decryptedUser;
var decryptedPwd;


exports.handler = function(event, context) {
    if (decryptedHost && decryptedUser && decryptedPwd) {
        console.log("Decrypted, getting brew info");
        getActiveBrew(event, context);
    } else {
        // Decrypt code should run once and variables stored outside of the function
        // handler so that these are decrypted once per container
        console.log("Decrypting...");
        var params = {
          Name: 'bcs_ip', /* required */
          WithDecryption: true
        };
        ssm.getParameter(params, function(err, data) {
          if (err) console.log(err, err.stack);
          else {
            decryptedHost = data.Parameter.Value;
            params.Name = 'bcs_user';
            ssm.getParameter(params, function(err, data) {
              if (err) console.log(err, err.stack); // an error occurred
              else {
                decryptedUser = data.Parameter.Value;
                params.Name = 'bcs_passwd';
                ssm.getParameter(params, function(err, data) {
                    if (err) console.log(err, err.stack); // an error occurred
                    else {
                        decryptedPwd = data.Parameter.Value;
                        getActiveBrew(event, context);
                    } 
                }); 
              } 
            });   
          }   
        });   
    }
};

function getActiveBrew(event, context) {
    let brewDate = 0;
    console.log("Getting brew info...");

    let params = {
        TableName: "brew_info"
    }

    dynamo.scan(params, function(err, data) {
        if (err) {
            console.log(err, err.stack); // an error occurred
        } else {
            console.log("Raw response from scan: " + JSON.stringify(data));
            let response = JSON.parse(JSON.stringify(data));
    
            let now = new Date().getTime();
            response.Items.forEach(function(item) {
                if (!item.brew_completion_date && item.yeast_pitch && now >= item.yeast_pitch.N) {
                    brewDate = item.brew_date.N;
                    console.log("Brew date: " + item.brew_date.N + " Yeast Pitch: " + item.yeast_pitch.N);
                    return true;
                }
            });
    
            if (brewDate) {
                processEvent(event, context, brewDate);
            } else {
                console.log("Failed to find active brew.  You should disable the pollers to avoid polling charges or create an active brew.");
                sendNotification("Failed to find active brew.  You should disable the pollers to avoid polling charges or create an active brew.");
            }
        }
    });
}

function processEvent(event, context, brewDate) {
    let results = [];
    let size = 2;
    for (let i = 0; i < size; i++) {
        let request = http.request({
                host: decryptedHost,
                auth: decryptedUser + ':' + decryptedPwd,
                path: '/api/temp/' + i,
                port: '8888',
                method: 'GET',
                timeout: SOCKET_TIMEOUT
            },
            (response) => {
                console.log('STATUS: ' + response.statusCode);
                console.log('HEADERS: ' + JSON.stringify(response.headers));
                response.setEncoding('utf8');
                let rawData = '';
                response.on('data', (chunk) => {
                    rawData += chunk;
                    console.log("data: " + rawData);
                });

                response.on('end', () => {
                    let parsedData = JSON.parse(rawData);
                    results.push(parsedData);
                    console.log("Data received from controller: " + JSON.stringify(parsedData));
                    console.log("Results: " + JSON.stringify(results));
                    recordReadings(results, size, brewDate);
                });
            }).on("error", (err) => {
                console.log("Error: " + err.message);
                sendNotification("Temperature Poller :: Error: " + err.message);
            }).on('timeout', () => {
                console.log("Request timed out obtaining BCS data.");
                request.abort();
                sendNotification("Temperature Poller :: Request timed out obtaining BCS data.");
            });
        request.end();
    }
}

function recordReadings(results, size, brewDate) {
    if (results.length === size) {
        let data = JSON.parse("{}");
        results.forEach(function(item) {
            let coarr = [];
            item.coefficients.forEach(function(coeff) {
                coarr.push({ N: coeff.toString() });
            });

            data[item.name] = { 
                M: { 
                    coefficients: { L: coarr }, 
                    enabled: {BOOL: item.enabled}, 
                    name: {S: item.name.toString()}, 
                    resistance: {N: item.resistance.toString()},
                    setpoint: {N: item.setpoint.toString()},
                    temp: {N: item.temp.toString()}
                }
            }            
        });

        let params = {
            TableName: 'brew_recordings',
            Item: {
                type: { S: 'temperature_recording' },
                timestamp: { N: new Date().getTime().toString() },
                brew_date: { N: brewDate },
                data: { M: data }
            }
        };

        let paramsStr = JSON.stringify(params);
        console.log("Params to persist: " + paramsStr);
        dynamo.putItem(params, function (err, resp) {
            let respMsg;
            if (err) {
                respMsg = err.message;
                sendNotification("Temperature Poller :: " + err.message);
            } else {
                respMsg = "Persisted item with response " + JSON.stringify(resp) + " for item: \n" + paramsStr;
            }

            console.log(respMsg);
        });
    }
}

function sendNotification(msg) {
    let sns = new AWS.SNS();

    sns.publish({
        Message: msg,
        TopicArn: IOT_SNS_TOPIC_ARN
    }, function(err, data) {
        if (err) console.log(err, err.stack);
        else console.log('Sent confirmation message: ' + msg);
    });
}