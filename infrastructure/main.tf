provider "aws" {
  // Use a profile for credentials, set in project.json
  region = "${var.region}"
}

data "aws_caller_identity" "current" { }

resource "aws_iam_role" "lambda_role" {
  name = "hopologybrewing-lambda"
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
  role_name = "hopologybrewing-lambda"
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

resource "aws_kms_key" "hopologybrewing-key" {
  description = "Key used for sensitive info related to the hoplogybrewing brewery controller"
  policy = <<EOF
  {
    "Version": "2012-10-17",
    "Id": "hopologybrewing-key-policy",
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
    creator = "Bryan Landerman"
    purpose = "Key used for sensitive info related to the hoplogybrewing brewery controller"
  }

  depends_on = ["aws_iam_role.lambda_role"]
}

resource "aws_kms_alias" "hopologybrewing-key" {
  name          = "alias/hopologybrewing-key"
  target_key_id = "${aws_kms_key.hopologybrewing-key.key_id}"
  depends_on = ["aws_kms_key.hopologybrewing-key"]
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
    creator = "Bryan Landerman"
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
    creator = "Bryan Landerman"
    purpose = "Record temperature and output readings during fermentation and crashing"
  }
}

output "region" {
  value = "${var.region}"
}

output "hopologybrewing-key-arn" {
  value = "${aws_kms_key.hopologybrewing-key.arn}"
}