# Hopology Brewing BCS Controller

An AWS based lambda or spring integration poller with data storage in DynamoDB for a BCS controller.  Contains a simple Spring boot app to run on a Raspberry Pi or AWS EC2 for data visualization.

## Getting Started

These instructions will get you a copy of the project up and running in your AWS account for development and testing purposes. It is not intended to be a production project, and is for homebrewing use only.

### Prerequisites

In order to run this project, you'll need to have some experience building applications in Java and node.js and using the unix command line. Experience using AWS to develop applications is also important, as well as understanding serverless computing, no-sql databases and spring.
    
The things you need to install the software and how to install them:

*  AWS Account - You can get one for free
*  [AWS Command Line Interface](https://aws.amazon.com/cli/)
*  Apex framework/toolkit installed from http://apex.run
*  Terraform installed from https://www.terraform.io
*  A basic understanding of how to use the [AWS console](http://console.aws.amazon.com)
*  An understanding of the command line in linux or macosx
*  Maven

### Prerequisite tools installation
Install the AWS CLI from a terminal window

```
brew install awscli
```

Install Apex (see full directions on http://apex.run)

```
curl https://raw.githubusercontent.com/apex/apex/master/install.sh | sh
```

Install npm, the Node Package Manager.

```
brew install node
```

Ensure you have set up an Apex AWS user and that you know the key and secret key. The roles can be assigned by Apex as long as your user has the following permissoins attached in the IAM console:

```
AWSLambdaFullAccess
IAMFullAccess
AmazonDynamoDBFullAccess
AmazonAPIGatewayAdministrator
AWSKeyManagementServicePowerUser
```

Set up a local AWS profile for your AWS user using the key and secret key pair

```
aws configure --profile brewery_profile
```

Setup your command line by exporting your profile to the environment so terraform and aws tools work

```
export AWS_PROFILE=brewery_profile
```


### Building, Deploying and Running the code

Prepare for Lambda Function deployment
1. Initialize apex:
```
apex init
```

2. Update your project.json to include environment variables for:
```
"memory": 512,
"timeout": 1,
"role": "arn:aws:iam::<<THE AWS ACCOUNT WHERE THIS IS DEPLOYED>>:role/brewery-lambda-role",
"profile": "brewery_profile",
  "environment": {
    "BCS_IP": "<<THE IP OF YOUR BCS CONTROLLER>>",
    "USER": "<<THE USER ID FOR YOUR CONTROLLER>>",
    "PWD": "<<THE PASSWORD FOR YOUR CONTROLLER>>"
  },
  "kms_arn": "<<TO BE INSERTED LATER>>"
```

#### Deploy Infrastructure using terraform apex integration
1. Your AWS Profile should contain your region but if not, you can specify it in your variables.tf file in the infrastructure directory
2. Open main.tf in the infrastructure file and update the tags to reflect your AWS account preferences
3. Deploy infrastructure by first running a plan:
```
apex infra plan
```

If everything looks good, then stand up the infrastructure:
```
apex infra apply
```

4. Copy the "hopologybrewing-key-arn" that is output upon completion.

#### Deploy Lambda Functions
1. Open your project.json and update the kms_arn with the value copied upon completion of the infrastructure deployment
2. Confirm what will be deployed:
```
apex deploy --dry-run
```
3. If all looks good, deploy your functions:
```
apex deploy
```
5. Open the variables.tf file and uncomment the *variable "apex_function_brew_info_put" {}* line.
6. Open the main.tf file and navigate to the bottom.  You'll notice several resource blocks commented out, uncomment them and re-run *apex infra apply*.  This will need to be repeated everytime a clean apply/deploy occurs.
7. Log into the AWS console and navigate to your lambda functions.  You will need to select the kms key and manually encrypt each environment variable and save the function.  For some reason Apex doesn't do this automatically, issue discussed here: https://github.com/apex/apex/issues/651

#### Remove your AWS resources:
1. Delete your functions:
```
apex delete
```

2. Destroy your infrastructure
```
apex infra destroy
```
3. Open the variables.tf file and comment out the *variable "apex_function_brew_info_put" {}* line.
4. Open the main.tf file and navigate to the bottom.  You'll notice several resource blocks under the comment *Consolidated items that require functions to be deployed by apex prior to "apex infra apply" executing successfully*.  Comment out all resource blocks below that comment. 

## Built With

* [Amazon Lambda](https://aws.amazon.com/lambda/) - Serverless execution for polling the controller
* [Apex](http://apex.run) - A framework to make deploying and managing the lambdas and infrastructure easy
* [Amazon DynamoDB](https://aws.amazon.com/dynamodb/) - Database as a Service for maintaining the party plan
* [Terraform](https://www.terraform.io/) - Infrastructure as Code, so we can tear down the infrastructure when a brew isn't fermenting

## Authors
* **Bryan Landerman**

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* ...
