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

resource "aws_iam_role_policy_attachment" "ssm_policy_attach" {
  role = "${data.aws_iam_role.lambda_role.role_name}"
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMReadOnlyAccess"
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

resource "aws_sns_topic" "iot_button_topic" {
  name = "iot_button_topic"
}

output "sns_iot_topic_arn" {
  value = "${aws_sns_topic.iot_button_topic.arn}"
}

output "region" {
  value = "${var.region}"
}

///////////////
TODO - Add tf to programatically add SSM Parameters for BCS data!
///////////////

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
//resource "aws_cloudwatch_event_target" "temperature_poll_every_five_minutes" {
//  rule = "${aws_cloudwatch_event_rule.every_five_minutes.name}"
//  arn = "${var.apex_function_temperature_poller}"
//}
//
//resource "aws_lambda_permission" "allow_cloudwatch_temperature_poll" {
//  statement_id = "AllowExecutionFromCloudWatch"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_temperature_poller}"
//  principal = "events.amazonaws.com"
//  source_arn = "${aws_cloudwatch_event_rule.every_five_minutes.arn}"
//}
//
//resource "aws_cloudwatch_event_target" "output_poll_every_minute" {
//  rule = "${aws_cloudwatch_event_rule.every_minute.name}"
//  arn = "${var.apex_function_output_poller}"
//}
//
//resource "aws_lambda_permission" "allow_cloudwatch_output_poll" {
//  statement_id = "AllowExecutionFromCloudWatch"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_output_poller}"
//  principal = "events.amazonaws.com"
//  source_arn = "${aws_cloudwatch_event_rule.every_minute.arn}"
//}
//
////------------
//// API Gateway Resources
////------------
//
//// Brew Info API
//resource "aws_api_gateway_rest_api" "brew_info_api" {
//  name        = "brew_info_api"
//  description = "API to manage brew info"
//}
//
//// Brew Creation Resource
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
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api", "aws_api_gateway_resource.brew_creation_resource"]
//}
//
//module "brew_creation_cors" {
//  source = "github.com/kevinthorley/terraform-api-gateway-cors-module"
//  resource_name = "brew_creation_cors"
//  resource_id = "${aws_api_gateway_resource.brew_creation_resource.id}"
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//}
//
//resource "aws_lambda_permission" "brew_info_create_permission" {
//  statement_id = "AllowBrewInfoCreateFromAPIGateway"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_brew_info_put}"
//  principal = "apigateway.amazonaws.com"
//  source_arn = "arn:aws:execute-api:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.brew_info_api.id}/*/${aws_api_gateway_method.brew_creation_post_method.http_method}${aws_api_gateway_resource.brew_creation_resource.path}"
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
//// Brew Update Resource
//resource "aws_api_gateway_resource" "brew_update_resource" {
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  parent_id   = "${aws_api_gateway_rest_api.brew_info_api.root_resource_id}"
//  path_part   = "update"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api"]
//}
//
//resource "aws_api_gateway_method" "brew_update_post_method" {
//  rest_api_id   = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  resource_id   = "${aws_api_gateway_resource.brew_update_resource.id}"
//  http_method   = "POST"
//  authorization = "NONE"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api", "aws_api_gateway_resource.brew_update_resource"]
//}
//
//module "brew_update_cors" {
//  source = "github.com/kevinthorley/terraform-api-gateway-cors-module"
//  resource_name = "brew_update_cors"
//  resource_id = "${aws_api_gateway_resource.brew_update_resource.id}"
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//}
//
//resource "aws_lambda_permission" "brew_info_update_permission" {
//  statement_id = "AllowBrewInfoUpdateFromAPIGateway"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_brew_info_update}"
//  principal = "apigateway.amazonaws.com"
//  source_arn = "arn:aws:execute-api:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.brew_info_api.id}/*/${aws_api_gateway_method.brew_update_post_method.http_method}${aws_api_gateway_resource.brew_update_resource.path}"
//  depends_on = ["aws_api_gateway_rest_api.brew_info_api"]
//}
//
//resource "aws_api_gateway_integration" "brew_update_post_lambda_integration" {
//  rest_api_id   = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  resource_id   = "${aws_api_gateway_resource.brew_update_resource.id}"
//  http_method = "${aws_api_gateway_method.brew_update_post_method.http_method}"
//  integration_http_method = "POST"
//  type = "AWS_PROXY"
//  uri = "arn:aws:apigateway:${var.region}:lambda:path/2015-03-31/functions/${var.apex_function_brew_info_update}/invocations"
//  depends_on = ["aws_api_gateway_method.brew_update_post_method"]
//}
//
//resource "aws_api_gateway_deployment" "brew_info_api_deployment" {
//  depends_on = ["aws_api_gateway_integration.brew_creation_post_lambda_integration", "aws_api_gateway_integration.brew_update_post_lambda_integration"]
//  rest_api_id = "${aws_api_gateway_rest_api.brew_info_api.id}"
//  stage_name = "api"
//
//  stage_description = "${timestamp()}" // hack to get deployment to update
//}
//
//output "brew_info_api_url" {
//  value = "https://${aws_api_gateway_deployment.brew_info_api_deployment.rest_api_id}.execute-api.${var.region}.amazonaws.com/${aws_api_gateway_deployment.brew_info_api_deployment.stage_name}"
//}
//
//// Cloudwatch API
//resource "aws_api_gateway_rest_api" "cloudwatch_api" {
//  name        = "cloudwatch_api"
//  description = "API to manage and read cloudwatch events"
//}
//
//// Cloudwatch Trigger Resource
//resource "aws_api_gateway_resource" "cloudwatch_trigger_resource" {
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  parent_id   = "${aws_api_gateway_rest_api.cloudwatch_api.root_resource_id}"
//  path_part   = "trigger"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api"]
//}
//
//resource "aws_api_gateway_method" "cloudwatch_trigger_post_method" {
//  rest_api_id   = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  resource_id   = "${aws_api_gateway_resource.cloudwatch_trigger_resource.id}"
//  http_method   = "POST"
//  authorization = "NONE"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api", "aws_api_gateway_resource.cloudwatch_trigger_resource"]
//}
//
//module "cloudwatch_trigger_cors" {
//  source = "github.com/kevinthorley/terraform-api-gateway-cors-module"
//  resource_name = "cloudwatch_trigger_cors"
//  resource_id = "${aws_api_gateway_resource.cloudwatch_trigger_resource.id}"
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//}
//
//resource "aws_lambda_permission" "cloudwatch_trigger_permission" {
//  statement_id = "AllowCloudwatchTriggerFromAPIGateway"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_manage_cloudwatch_triggers}"
//  principal = "apigateway.amazonaws.com"
//  source_arn = "arn:aws:execute-api:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.cloudwatch_api.id}/*/${aws_api_gateway_method.cloudwatch_trigger_post_method.http_method}${aws_api_gateway_resource.cloudwatch_trigger_resource.path}"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api"]
//}
//
//resource "aws_api_gateway_integration" "cloudwatch_trigger_post_lambda_integration" {
//  rest_api_id   = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  resource_id   = "${aws_api_gateway_resource.cloudwatch_trigger_resource.id}"
//  http_method = "${aws_api_gateway_method.cloudwatch_trigger_post_method.http_method}"
//  integration_http_method = "POST"
//  type = "AWS_PROXY"
//  uri = "arn:aws:apigateway:${var.region}:lambda:path/2015-03-31/functions/${var.apex_function_manage_cloudwatch_triggers}/invocations"
//  depends_on = ["aws_api_gateway_method.cloudwatch_trigger_post_method"]
//}
//
//resource "aws_api_gateway_deployment" "cloudwatch_trigger_deployment" {
//  depends_on = ["aws_api_gateway_integration.cloudwatch_trigger_post_lambda_integration"]
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  stage_name = "api"
//
//  stage_description = "${timestamp()}" // hack to get deployment to update
//}
//
//output "cloudwatch_api_url" {
//  value = "https://${aws_api_gateway_deployment.cloudwatch_trigger_deployment.rest_api_id}.execute-api.${var.region}.amazonaws.com/${aws_api_gateway_deployment.cloudwatch_trigger_deployment.stage_name}"
//}
//
//// Cloudwatch List Resource
//resource "aws_api_gateway_resource" "cloudwatch_list_resource" {
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  parent_id   = "${aws_api_gateway_rest_api.cloudwatch_api.root_resource_id}"
//  path_part   = "list"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api"]
//}
//
//resource "aws_api_gateway_method" "cloudwatch_list_method" {
//  rest_api_id   = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  resource_id   = "${aws_api_gateway_resource.cloudwatch_list_resource.id}"
//  http_method   = "GET"
//  authorization = "NONE"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api", "aws_api_gateway_resource.cloudwatch_list_resource"]
//}
//
//module "cloudwatch_list_cors" {
//  source = "github.com/kevinthorley/terraform-api-gateway-cors-module"
//  resource_name = "cloudwatch_list_cors"
//  resource_id = "${aws_api_gateway_resource.cloudwatch_list_resource.id}"
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//}
//
//resource "aws_lambda_permission" "cloudwatch_list_permission" {
//  statement_id = "AllowCloudwatchListFromAPIGateway"
//  action = "lambda:InvokeFunction"
//  function_name = "${var.apex_function_get_cloudwatch_triggers}"
//  principal = "apigateway.amazonaws.com"
//  source_arn = "arn:aws:execute-api:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_api_gateway_rest_api.cloudwatch_api.id}/*/${aws_api_gateway_method.cloudwatch_list_method.http_method}${aws_api_gateway_resource.cloudwatch_list_resource.path}"
//  depends_on = ["aws_api_gateway_rest_api.cloudwatch_api"]
//}
//
//resource "aws_api_gateway_integration" "cloudwatch_list_lambda_integration" {
//  rest_api_id   = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  resource_id   = "${aws_api_gateway_resource.cloudwatch_list_resource.id}"
//  http_method = "${aws_api_gateway_method.cloudwatch_list_method.http_method}"
//  integration_http_method = "POST"
//  type = "AWS_PROXY"
//  uri = "arn:aws:apigateway:${var.region}:lambda:path/2015-03-31/functions/${var.apex_function_get_cloudwatch_triggers}/invocations"
//  depends_on = ["aws_api_gateway_method.cloudwatch_list_method"]
//}
//
//resource "aws_api_gateway_deployment" "cloudwatch_get_deployment" {
//  depends_on = ["aws_api_gateway_integration.cloudwatch_list_lambda_integration"]
//  rest_api_id = "${aws_api_gateway_rest_api.cloudwatch_api.id}"
//  stage_name = "api"
//
//  stage_description = "${timestamp()}" // hack to get deployment to update
//}
