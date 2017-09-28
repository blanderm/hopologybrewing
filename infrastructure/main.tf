provider "aws" {
  // Use a profile for credentials, set in project.json
  region = "${var.region}"
}

data "aws_caller_identity" "current" { }

resource "aws_iam_role" "lambda_role" {
  name = "brewery-lambda-role"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    },
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "apigateway.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
}

data "aws_iam_role" "lambda_role" {
  role_name = "brewery-lambda-role"
  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_iam_role_policy_attachment" "cloudwatch_policy_attach" {
  role = "${data.aws_iam_role.lambda_role.role_name}"
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchLogsFullAccess"
  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_iam_role_policy_attachment" "dynamo_policy_attach" {
  role = "${data.aws_iam_role.lambda_role.role_name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_iam_role_policy_attachment" "lambda_policy_attach" {
  role = "${data.aws_iam_role.lambda_role.role_name}"
  policy_arn = "arn:aws:iam::aws:policy/AWSLambdaFullAccess"
  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_iam_role_policy_attachment" "api_gateway_policy_attach" {
  role = "${data.aws_iam_role.lambda_role.role_name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonAPIGatewayAdministrator"
  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_iam_policy" "sns_allow_policy" {
  name        = "sns-allow-policy"
  description = "Allow function to interact with SNS"
  policy      =  <<EOF
{
  "Id": "Policy1506534119126",
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "Stmt1506533767150",
      "Action": [
        "sns:CreateTopic",
        "sns:ListSubscriptions",
        "sns:Publish",
        "sns:Subscribe"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_iam_policy_attachment" "sns_allow_policy_attach" {
  name       = "sns_allow_policy_attach"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "${aws_iam_policy.sns_allow_policy.arn}"
  depends_on = ["aws_iam_role.lambda_role", "aws_iam_policy.sns_allow_policy"]
}

resource "aws_iam_policy" "api_invoke_policy" {
  name        = "api_invoke_policy"
  description = "Allow APIs to post to Lambdas"
  policy      =  <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
          "Effect": "Allow",
          "Action": [
            "apigateway:GET",
            "apigateway:POST"
          ],
          "Resource": [
            "arn:aws:apigateway:${var.region}::/restapis"
          ]
        }
    ]
}
EOF
}

resource "aws_iam_policy_attachment" "api_invoke_policy_attach" {
  name       = "api_invoke_policy_attach"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "${aws_iam_policy.api_invoke_policy.arn}"
  depends_on = ["aws_iam_role.lambda_role", "aws_iam_policy.api_invoke_policy"]
}

resource "aws_kms_key" "brewery_key" {
  description = "Key used for sensitive info related to the brewery controller"
  policy = <<EOF
  {
    "Version": "2012-10-17",
    "Id": "brewery-key-policy",
    "Statement": [
      {
        "Sid": "Enable IAM User Permissions",
        "Effect": "Allow",
        "Principal": {
          "AWS": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        },
        "Action": "kms:*",
        "Resource": "*"
      },
      {
        "Sid": "Allow access for Key Administrators",
        "Effect": "Allow",
        "Principal": {
          "AWS": "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/${basename(data.aws_caller_identity.current.arn)}"
        },
        "Action": [
          "kms:Create*",
          "kms:Describe*",
          "kms:Enable*",
          "kms:List*",
          "kms:Put*",
          "kms:Update*",
          "kms:Revoke*",
          "kms:Disable*",
          "kms:Get*",
          "kms:Delete*",
          "kms:TagResource",
          "kms:UntagResource",
          "kms:ScheduleKeyDeletion",
          "kms:CancelKeyDeletion"
        ],
        "Resource": "*"
      },
      {
        "Sid": "Allow use of the key",
        "Effect": "Allow",
        "Principal": {
          "AWS": [
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${data.aws_iam_role.lambda_role.role_name}",
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/${basename(data.aws_caller_identity.current.arn)}"
          ]
        },
        "Action": [
          "kms:Encrypt",
          "kms:Decrypt",
          "kms:ReEncrypt*",
          "kms:GenerateDataKey*",
          "kms:DescribeKey"
        ],
        "Resource": "*"
      },
      {
        "Sid": "Allow attachment of persistent resources",
        "Effect": "Allow",
        "Principal": {
          "AWS": [
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:role/${data.aws_iam_role.lambda_role.role_name}",
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/${basename(data.aws_caller_identity.current.arn)}"
          ]
        },
        "Action": [
          "kms:CreateGrant",
          "kms:ListGrants",
          "kms:RevokeGrant"
        ],
        "Resource": "*",
        "Condition": {
          "Bool": {
            "kms:GrantIsForAWSResource": "true"
          }
        }
      }
    ]
  }
  EOF

  tags {
    name = "Brewery Temperature Controller"
    creator = "${data.aws_caller_identity.current.account_id}"
    purpose = "Key used for sensitive info related to the brewery controller"
  }

  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_kms_alias" "brewery_key" {
  name          = "alias/brewery_key"
  target_key_id = "${aws_kms_key.brewery_key.key_id}"
  depends_on = ["aws_kms_key.brewery_key"]
}

// DynamoDB resources
resource "aws_dynamodb_table" "brew_info" {
  name = "brew_info"
  read_capacity = 1
  write_capacity = 1
  hash_key = "brew_date"

  attribute {
    name = "brew_date"
    type = "N"
  }

  tags {
    name = "Brewery Temperature Controller"
    creator = "${data.aws_caller_identity.current.account_id}"
    purpose = "Defines a brew including key dates."
  }
}

resource "aws_dynamodb_table" "brew_recordings" {
  name = "brew_recordings"
  read_capacity = 30
  write_capacity = 2
  hash_key = "type"
  range_key = "timestamp"

  attribute {
    name = "type"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "N"
  }

  attribute {
    name = "brew_date"
    type = "N"
  }

  global_secondary_index {
    name               = "brew_date-index"
    hash_key           = "brew_date"
    write_capacity     = 1
    read_capacity      = 1
    projection_type    = "ALL"
  }

  tags {
    name = "Brewery Temperature Controller"
    creator = "${data.aws_caller_identity.current.account_id}"
    purpose = "Record temperature and output readings during fermentation and crashing"
  }
}

output "region" {
  value = "${var.region}"
}

output "brewery-key-arn" {
  value = "${aws_kms_key.brewery_key.arn}"
}

////
// Consolidated items that require functions to be deployed by apex prior to "apex infra apply" executing successfully
////

//resource "aws_cloudwatch_event_rule" "every_minute" {
//  name = "every-minute"
//  description = "Fires every minute"
//  schedule_expression = "rate(1 minute)"
//  is_enabled = false
//}
//
//resource "aws_cloudwatch_event_rule" "every_five_minutes" {
//  name = "every-five-minutes"
//  description = "Fires every five minutes"
//  schedule_expression = "rate(5 minutes)"
//  is_enabled = false
//}
//
//resource "aws_cloudwatch_event_target" "temperature_poll_every_minute" {
//  rule = "${aws_cloudwatch_event_rule.every_minute.name}"
//  arn = "${var.apex_function_temperature_poller}"
//}
//
//resource "aws_lambda_permission" "allow_cloudwatch_temperature_poll" {
//  statement_id = "AllowExecutionFromCloudWatch"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_temperature_poller}"
//  principal = "events.amazonaws.com"
//  source_arn = "${aws_cloudwatch_event_rule.every_minute.arn}"
//}
//
//resource "aws_cloudwatch_event_target" "output_poll_every_five_minutes" {
//  rule = "${aws_cloudwatch_event_rule.every_five_minutes.name}"
//  arn = "${var.apex_function_output_poller}"
//}
//
//resource "aws_lambda_permission" "allow_cloudwatch_output_poll" {
//  statement_id = "AllowExecutionFromCloudWatch"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_output_poller}"
//  principal = "events.amazonaws.com"
//  source_arn = "${aws_cloudwatch_event_rule.every_five_minutes.arn}"
//}
//
//// API Gateway Resources
//resource "aws_api_gateway_rest_api" "brew_info_api" {
//  name        = "brew_info_api"
//  description = "API to manage brew info"
//}
//
//resource "aws_api_gateway_resource" "brew_creation_resource" {
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  parent_id   = "${aws_api_gateway_rest_api.brew_info_api.root_resource_id}"
//  path_part   = "create"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api"]
//}
//
//resource "aws_api_gateway_method" "brew_creation_post_method" {
//  rest_api_id   = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  resource_id   = "${aws_api_gateway_resource.brew_creation_resource.id}"
//  http_method   = "POST"
//  authorization = "NONE"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api"]
//}
//
//module "brew_creation_cors" {
//  source = "github.com/kevinthorley/terraform-api-gateway-cors-module"
//  resource_name = "brew_creation_cors"
//  resource_id = "${aws_api_gateway_resource.brew_creation_resource.id}"
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//}
//
//resource "aws_lambda_permission" "brew_info_apigateway_permission" {
//  statement_id = "AllowExecutionFromAPIGateway"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_brew_info_put}"
//  principal = "apigateway.amazonaws.com"
//  source_arn = "arn:aws:execute-api:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.brew_info_api.id}/*/*"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api"]
//}
//
//resource "aws_api_gateway_integration" "brew_creation_post_lambda_integration" {
//  rest_api_id   = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  resource_id   = "${aws_api_gateway_resource.brew_creation_resource.id}"
//  http_method = "${aws_api_gateway_method.brew_creation_post_method.http_method}"
//  integration_http_method = "POST"
//  type = "AWS_PROXY"
//  uri = "arn:aws:apigateway:${var.region}:lambda:path/2015-03-31/functions/${var.apex_function_brew_info_put}/invocations"
//  depends_on = ["aws_api_gateway_method.brew_creation_post_method"]
//}
//
//resource "aws_api_gateway_deployment" "brew_info_api_deployment" {
//  depends_on = ["aws_api_gateway_integration.brew_creation_post_lambda_integration"]
//  //, "aws_api_gateway_integration.brew_creation_options_lambda_integration"]
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  stage_name = "api"
//
//  stage_description = "${timestamp()}" // hack to get deployment to update
//}
//
//output "brew_creation_api_url" {
//  value = "https://${aws_api_gateway_deployment.brew_info_api_deployment.rest_api_id}.execute-api.${var.region}.amazonaws.com/${aws_api_gateway_deployment.brew_info_api_deployment.stage_name}"
//}
//
//output "brew_info_put_arn" {
//  value = "${var.apex_function_brew_info_put}"
//}