'use strict';

const http = require('http');
const AWS = require("com.hopologybrewing.bcs.capture.aws-sdk");

const encryptedHost = process.env['bcs_ip'];
const encryptedUser = process.env['user'];
const encryptedPassword = process.env['pwd'];
let decryptedHost;
let decryptedUser;
let decryptedPwd;


exports.handler = (event, context, callback) => {
    if (decryptedHost && decryptedUser && decryptedPwd) {
        processEvent(event, context, callback);
    } else {
        // Decrypt code should run once and variables stored outside of the function
        // handler so that these are decrypted once per container
        const kms = new AWS.KMS();
        kms.decrypt({ CiphertextBlob: new Buffer(encryptedHost, 'base64') }, (err, data) => {
            if (err) {
                console.log('Decrypt error:', err);
                return callback(err);
            }
            decryptedHost = data.Plaintext.toString('ascii');
    });

        kms.decrypt({ CiphertextBlob: new Buffer(encryptedUser, 'base64') }, (err, data) => {
            if (err) {
                console.log('Decrypt error:', err);
                return callback(err);
            }
            decryptedUser = data.Plaintext.toString('ascii');
    });

        kms.decrypt({ CiphertextBlob: new Buffer(encryptedPassword, 'base64') }, (err, data) => {
            if (err) {
                console.log('Decrypt error:', err);
                return callback(err);
            }
            decryptedPwd = data.Plaintext.toString('ascii');
        processEvent(event, context, callback);
    });
    }
};

function processEvent(event, context, callback) {
    console.log("IP: " + decryptedHost);

    var docClient = new AWS.DynamoDB.DocumentClient();

    var table = "beer_fermentation";
    var id = "current";

    var params = {
        TableName: table,
        Key:{
            "brew_date": id
        }
    };

    docClient.get(params, function(err, data) {
        if (err) {
            console.error("Unable to read item. Error JSON:", JSON.stringify(err, null, 2));
        } else {
            var json = JSON.parse(JSON.stringify(data, null, 2));
            console.log("GetItem succeeded:", json);
            console.log("GetItem succeeded:", json.Item.latest_brew);
        }
    });

    var request = http.request({
            'host': decryptedHost,
            'auth': decryptedUser + ':' + decryptedPwd,
            'path': '/api/temp/0'
        },
        function (response) {
            console.log('STATUS: ' + response.statusCode);
            console.log('HEADERS: ' + JSON.stringify(response.headers));
            response.setEncoding('utf8');
            var rawData = '';
            response.on('data', function (chunk) {
                rawData += chunk
                console.log('BODY: ' + rawData);
            });

            response.on('end', () => {
                try {
                    var parsedData = JSON.parse(rawData);
            console.log(parsedData);
        } catch (e) {
                console.log(e.message);
            }
        });
        });
    request.end();

    callback(null, 'Hello from Lambda');
};