ALTER TABLE users ADD COLUMN payment_handles JSONB DEFAULT '{}';
COMMENT ON COLUMN users.payment_handles IS 'User payment app handles (venmo, paypal, upi, etc.) for deep link integration';
