# Billing & Credits

## Credit System

The application uses a **credit-based pricing model**:

- **1 credit = 1 ledger analysis**
- Credits are purchased via Razorpay (one-time payments, not subscriptions)
- Plans are DB-driven (see `plans` table in auth-service V1 migration)

### Plans

| Plan  | Credits | Price (INR) | Trial? |
|-------|---------|-------------|--------|
| Trial | 2       | ₹0          | Yes    |
| Pro   | 5       | ₹1,000      | No     |
| Ultra | 30      | ₹3,000      | No     |

### Architecture

- **All credit/payment logic lives in `auth-service`** under `com.learning.authservice.credit.*`
- **`backend-service`** calls `auth-service` via WebClient (`CreditClient`) for credit consumption
- Trial credits are auto-granted on signup via `GrantTrialCreditsAction` in the signup pipeline

### API Endpoints

| Endpoint                              | Method | Auth     | Purpose                          |
|---------------------------------------|--------|----------|----------------------------------|
| `/auth/api/v1/plans`                  | GET    | Public   | List active pricing plans        |
| `/auth/api/v1/credits`               | GET    | JWT      | Get user's wallet balance        |
| `/auth/api/v1/credits/consume`       | POST   | Internal | Deduct credits (called by backend) |
| `/auth/api/v1/credits/grant`         | POST   | Internal | Grant credits (admin/system)     |
| `/auth/api/v1/payments/create-order` | POST   | JWT      | Create Razorpay order            |
| `/auth/api/v1/payments/verify`       | POST   | JWT      | Verify payment & grant credits   |

### Payment Flow

```
Frontend → POST /payments/create-order → Razorpay Order
Frontend → Razorpay Checkout Widget
Frontend → POST /payments/verify (orderId, paymentId, signature)
Backend  → HMAC verification → Grant credits → Return wallet
```


### Managing Plans

Plans are stored in the `plans` table in the `auth` database. There is currently no admin UI for managing plans, so updates must be done via SQL.

#### View Current Plans
```sql
SELECT * FROM plans ORDER BY price_inr ASC;
```

#### Update Plan Credits (e.g., Change Ultra to 20 credits)
To change the 'Ultra' plan from 30 credits to 20:

```sql
UPDATE plans 
SET credits = 20, 
    description = '20 ledger analyses' 
WHERE name = 'ultra';
```

#### Add a New Plan
```sql
INSERT INTO plans (name, display_name, price_inr, credits, is_trial, description, active, created_at, updated_at)
VALUES ('enterprise', 'Enterprise', 10000.00, 150, false, '150 ledger analyses', true, NOW(), NOW());
```

---

## Going Live with Razorpay

To switch from Test Mode to Live Mode:

### 1. Razorpay Dashboard
1. Log in to your [Razorpay Dashboard](https://dashboard.razorpay.com/).
2. Toggle the switch in the top-right corner from **Test Mode** to **Live Mode**.
3. Go to **Settings** → **API Keys**.
4. Generate a new **Key ID** and **Key Secret** for Live Mode. **Save these immediately** as the secret is shown only once.

### 2. Update Application Configuration
Update your environment variables (in `.env`, `docker-compose.yml`, or your deployment secrets) with the Live keys.

```bash
# .env file
RAZORPAY_KEY_ID=rzp_live_xxxxxxxxxxxxxx  # Replace with your LIVE Key ID
RAZORPAY_KEY_SECRET=yyyyyyyyyyyyyyyyyyyy  # Replace with your LIVE Key Secret
RAZORPAY_WEBHOOK_SECRET=zzzzzzzzzzzzzzzz  # Optional, if using webhooks later
```

### 3. Verification
1. Restart the `auth-service` to pick up the new keys.
2. The frontend automatically fetches the Key ID from the backend (`/auth/api/v1/payments/key`).
3. Make a small purchase (e.g., ₹1,000 Pro plan) using a real card/UPI.
4. Verify the payment appears in your **Live** Razorpay Dashboard.
5. Verify credits are added to your wallet on the dashboard.

### 4. Important Notes
- **Currencies**: Ensure your Razorpay account supports **INR**.
- **Webhooks**: While currently the backend verifies payments via API immediately after the frontend success callback, setting up Webhooks is recommended for robustness (e.g., if the user closes the browser before the callback functionality completes).
- **Refunds**: If you need to test refunds, you can initiative them from the Razorpay Dashboard.
