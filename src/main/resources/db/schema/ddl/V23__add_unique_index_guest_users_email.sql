CREATE UNIQUE INDEX idx_guest_users_email_unique ON guest_users(email)
  WHERE email IS NOT NULL;
