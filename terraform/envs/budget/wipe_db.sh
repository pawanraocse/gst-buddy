#!/bin/bash
PROJECT_NAME="gst-buddy"
ENVIRONMENT="budget"
AWS_REGION="us-east-1"
DB_HOST=$(aws ssm get-parameter --name "/${PROJECT_NAME}/${ENVIRONMENT}/rds/endpoint" --query 'Parameter.Value' --output text --region "$AWS_REGION")
DB_NAME=$(aws ssm get-parameter --name "/${PROJECT_NAME}/${ENVIRONMENT}/rds/database" --query 'Parameter.Value' --output text --region "$AWS_REGION")
DB_USERNAME=$(aws ssm get-parameter --name "/${PROJECT_NAME}/${ENVIRONMENT}/rds/username" --query 'Parameter.Value' --output text --region "$AWS_REGION")
SECRET_ARN=$(aws ssm get-parameter --name "/${PROJECT_NAME}/${ENVIRONMENT}/rds/secret_arn" --query 'Parameter.Value' --output text --region "$AWS_REGION")
DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" --query 'SecretString' --output text --region "$AWS_REGION" | jq -r '.password')

docker run --rm -e PGPASSWORD=$DB_PASSWORD postgres:15 psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
