-- V2: Add discount/sale support to pricing plans
ALTER TABLE plans ADD COLUMN sale_price_inr DECIMAL(10,2);
ALTER TABLE plans ADD COLUMN is_sale_active BOOLEAN NOT NULL DEFAULT false;

-- Apply 20% Discount Sale (Rounded for Premium Feel)
-- Pro: 149 -> 119
UPDATE plans SET sale_price_inr = 119.00, is_sale_active = true WHERE name = 'pro';

-- Ultra: 999 -> 799
UPDATE plans SET sale_price_inr = 799.00, is_sale_active = true WHERE name = 'ultra';

-- Max: 3499 -> 2799
UPDATE plans SET sale_price_inr = 2799.00, is_sale_active = true WHERE name = 'max';
