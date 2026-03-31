---
description: Run diagnostic checks for the prod_init environment, including EC2 status, logs, and Amplify build progress.
---

# /debug (Prod Init Troubleshooting)

Follow these steps to diagnose issues in the production environment:

1. **Check EC2 Service Status**: 
   - Command: `ssh -o StrictHostKeyChecking=no -i ~/.ssh/id_rsa_personal ec2-user@65.1.250.35 "sudo -E /usr/local/bin/docker-compose -p gstbuddies -f /app/docker-compose.prod_init.yml ps"`

2. **Tail auth-service logs for errors**:
   - Command: `ssh -o StrictHostKeyChecking=no -i ~/.ssh/id_rsa_personal ec2-user@65.1.250.35 "sudo -E /usr/local/bin/docker-compose -p gstbuddies -f /app/docker-compose.prod_init.yml logs auth-service --tail 100"`

3. **Check Amplify Build Status**:
   - Command: `aws amplify list-jobs --app-id d3mnggqk8iw31y --branch-name prod --max-items 1 --region ap-south-1`

4. **Verify SSM Secret (Razorpay)**:
   - Command: `aws ssm get-parameter --name "/gstbuddies/prod_init/razorpay/key_id" --with-decryption --query "Parameter.Value" --output text --region ap-south-1`
