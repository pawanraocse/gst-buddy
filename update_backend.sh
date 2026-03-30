#!/bin/bash
set -e
EC2_IP="3.82.180.145"
SSH_KEY=~/.ssh/id_rsa_personal

ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" << 'REMOTE_SETUP'
    set -e
    sudo yum install -y rsync git jq docker
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker ec2-user
    sudo mkdir -p /usr/local/lib/docker/cli-plugins
    sudo curl -SL "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-$(uname -m)" -o /usr/local/lib/docker/cli-plugins/docker-compose
    sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
    sudo curl -SL "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-linux-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    sudo mkdir -p /app && sudo chown ec2-user:ec2-user /app
REMOTE_SETUP

rsync -avz --progress -e "ssh -o StrictHostKeyChecking=no -i $SSH_KEY" \
    --exclude '.terraform' \
    --exclude 'node_modules' \
    --exclude '.git' \
    --exclude '.idea' \
    --exclude 'frontend' \
    --exclude '*.class' \
    --exclude 'target/classes' \
    --exclude 'target/test-classes' \
    --exclude 'target/generated-sources' \
    --exclude 'target/maven-status' \
    --include 'target/*.jar' \
    /home/pawan/personal/gst-buddy/ ec2-user@"$EC2_IP":/app/

ssh -o StrictHostKeyChecking=no -i "$SSH_KEY" ec2-user@"$EC2_IP" "cd /app && chmod +x scripts/prod_init/*.sh && ./scripts/prod_init/start.sh"

aws amplify start-job --app-id d3tdjwycaeadss --branch-name prod --job-type RELEASE --region us-east-1
