#!/bin/bash
echo "=== USERS ==="
docker exec gstbuddies-auth-db-1 psql -U authuser -d authdb -c "SELECT user_id, email, status, source FROM users WHERE email LIKE '%pawanraocse%';"
echo ""
echo "=== USER CREDITS ==="
docker exec gstbuddies-auth-db-1 psql -U authuser -d authdb -c "SELECT * FROM user_credits;"
