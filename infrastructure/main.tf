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

resource "aws_iam_policy" "sns-allow-policy" {
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

resource "aws_iam_policy_attachment" "sns_policy_attach" {
  name       = "sns-policy-attach"
  roles      = ["${aws_iam_role.lambda_role.name}"]
  policy_arn = "${aws_iam_policy.sns-allow-policy.arn}"
  depends_on = ["aws_iam_role.lambda_role", "aws_iam_policy.sns-allow-policy"]
}

resource "aws_kms_key" "brewery-key" {
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

resource "aws_kms_alias" "brewery-key" {
  name          = "alias/brewery-key"
  target_key_id = "${aws_kms_key.brewery-key.key_id}"
  depends_on = ["aws_kms_key.brewery-key"]
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
  value = "${aws_kms_key.brewery-key.arn}"
}