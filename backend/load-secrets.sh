#!/bin/bash

set -e

SECRET_JSON=$(aws secretsmanager get-secret-value \
  --secret-id expirymate/prod/backend \
  --region us-east-2 \
  --query SecretString \
  --output text)

export DB_PASSWORD=$(echo "$SECRET_JSON" | jq -r '.DB_PASSWORD')
export JWT_SECRET=$(echo "$SECRET_JSON" | jq -r '.JWT_SECRET')
export RABBITMQ_PASSWORD=$(echo "$SECRET_JSON" | jq -r '.RABBITMQ_PASSWORD')
export SES_SMTP_USERNAME=$(echo "$SECRET_JSON" | jq -r '.SES_SMTP_USERNAME')
export SES_SMTP_PASSWORD=$(echo "$SECRET_JSON" | jq -r '.SES_SMTP_PASSWORD')