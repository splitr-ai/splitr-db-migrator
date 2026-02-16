CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for UUID generation

CREATE TABLE users (
                       id VARCHAR(60) PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       name VARCHAR(100) NOT NULL,
                       phone VARCHAR(20),
                       avatar_url TEXT,
                       default_currency CHAR(3) NOT NULL DEFAULT 'USD',

    -- Auth integration
                       cognito_sub VARCHAR(255) UNIQUE,  -- Links to AWS Cognito user pool

    -- Referral system
                       referral_code VARCHAR(20) UNIQUE NOT NULL DEFAULT UPPER(SUBSTRING(gen_random_uuid()::text, 1, 8)),
                       referred_by VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,
                       referral_credits_cents BIGINT NOT NULL DEFAULT 0,

    -- Premium status
                       is_premium BOOLEAN NOT NULL DEFAULT FALSE,
                       premium_until TIMESTAMPTZ,

    -- Preferences (JSONB for flexibility)
                       preferences JSONB NOT NULL DEFAULT '{
                         "notifications": true,
                         "email_digest": "weekly",
                         "default_split_type": "equal"
                       }'::jsonb,

    -- Sync support columns
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                       version INTEGER NOT NULL DEFAULT 1,

    -- Constraints
                       CONSTRAINT chk_users_currency CHECK (default_currency ~ '^[A-Z]{3}$'),
                        CONSTRAINT chk_users_referral_credits CHECK (referral_credits_cents >= 0)
);

-- Index for referral lookups
CREATE INDEX idx_users_referral_code ON users(referral_code) WHERE NOT is_deleted;
CREATE INDEX idx_users_cognito_sub ON users(cognito_sub) WHERE cognito_sub IS NOT NULL;
CREATE INDEX idx_users_email ON users(email) WHERE NOT is_deleted;



-- -----------------------------------------------------------------------------
-- Guest Users: Non-registered participants who can view/add via web
-- -----------------------------------------------------------------------------
CREATE TABLE guest_users (
     id VARCHAR(60) PRIMARY KEY,
     email VARCHAR(255),
     name VARCHAR(100) NOT NULL,
     phone VARCHAR(20),

    -- If guest later registers, link to their user account
     converted_to_user_id VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,

    -- Token for guest access (signed, time-limited)
     access_token VARCHAR(255) UNIQUE,
     token_expires_at TIMESTAMPTZ,

    -- Interaction tracking for upgrade prompts
     interaction_count INTEGER NOT NULL DEFAULT 0,

    -- Sync support columns
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
     version INTEGER NOT NULL DEFAULT 1
);

CREATE INDEX idx_guest_users_email ON guest_users(email) WHERE email IS NOT NULL AND NOT is_deleted;
CREATE INDEX idx_guest_users_access_token ON guest_users(access_token) WHERE NOT is_deleted;


-- -----------------------------------------------------------------------------
-- Expense Groups: Primary organizational unit
-- -----------------------------------------------------------------------------
CREATE TABLE groups (
    id VARCHAR(60) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    default_currency CHAR(3) NOT NULL DEFAULT 'USD',

    -- Settings
    simplify_debts BOOLEAN NOT NULL DEFAULT TRUE,

    -- Invite mechanics
    invite_code VARCHAR(20) UNIQUE NOT NULL DEFAULT UPPER(SUBSTRING(gen_random_uuid()::text, 1, 8)),
    invite_code_expires_at TIMESTAMPTZ,  -- NULL = never expires

    -- Group image/avatar
    image_url TEXT,

    -- Archive instead of delete to preserve history
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,

    -- Creator
    created_by VARCHAR(60) NOT NULL REFERENCES users(id) ON DELETE RESTRICT,

    -- Sync support columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT chk_groups_currency CHECK (default_currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_groups_invite_code ON groups(invite_code) WHERE NOT is_deleted;
CREATE INDEX idx_groups_created_by ON groups(created_by) WHERE NOT is_deleted;


-- -----------------------------------------------------------------------------
-- Group Members: Junction table with roles
-- -----------------------------------------------------------------------------
CREATE TABLE group_members (
   id VARCHAR(60) PRIMARY KEY,
   group_id VARCHAR(60) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- Either a registered user or a guest (mutually exclusive)
   user_id VARCHAR(60) REFERENCES users(id) ON DELETE CASCADE,
   guest_user_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE CASCADE,

    -- Role management
   role VARCHAR(20) NOT NULL DEFAULT 'member',

    -- Display name override for this group
   display_name VARCHAR(100),

    -- Notification preferences for this group
   notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,

    -- Left group but preserve for historical balance calculation
   left_at TIMESTAMPTZ,

    -- Sync support columns
   joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
   is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
   version INTEGER NOT NULL DEFAULT 1,

    -- Constraints
   CONSTRAINT chk_group_members_role CHECK (role IN ('admin', 'member')),
   CONSTRAINT chk_group_members_user_or_guest CHECK (
       (user_id IS NOT NULL AND guest_user_id IS NULL) OR
       (user_id IS NULL AND guest_user_id IS NOT NULL)
       ),
    -- Unique membership per group
   CONSTRAINT uq_group_members_user UNIQUE (group_id, user_id),
   CONSTRAINT uq_group_members_guest UNIQUE (group_id, guest_user_id)
);

CREATE INDEX idx_group_members_group_id ON group_members(group_id) WHERE NOT is_deleted;
CREATE INDEX idx_group_members_user_id ON group_members(user_id) WHERE user_id IS NOT NULL AND NOT is_deleted;
CREATE INDEX idx_group_members_guest_id ON group_members(guest_user_id) WHERE guest_user_id IS NOT NULL AND NOT is_deleted;


-- -----------------------------------------------------------------------------
-- Categories: Expense categorization
-- -----------------------------------------------------------------------------
CREATE TABLE categories (
    id VARCHAR(60) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    icon VARCHAR(50),  -- Icon name/code for UI
    color VARCHAR(7),  -- Hex color code
    parent_id VARCHAR(60) REFERENCES categories(id) ON DELETE SET NULL,

    -- System vs user-created
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,

    -- Ordering
    sort_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_categories_color CHECK (color IS NULL OR color ~ '^#[0-9A-Fa-f]{6}$')
);

-- -----------------------------------------------------------------------------
-- Expenses: Main expense records
-- -----------------------------------------------------------------------------
CREATE TABLE expenses (
      id VARCHAR(60) PRIMARY KEY,
      group_id VARCHAR(60) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- Expense details
      description VARCHAR(255) NOT NULL,
      total_amount BIGINT NOT NULL,  -- Stored in smallest currency unit (cents)
      currency CHAR(3) NOT NULL,
      category_id VARCHAR(60) REFERENCES categories(id) ON DELETE SET NULL,
      expense_date DATE NOT NULL DEFAULT CURRENT_DATE,

    -- Split configuration
      split_type VARCHAR(20) NOT NULL,

    -- Optional metadata
      notes TEXT,

    -- Location (optional, for travel expenses)
      location_name VARCHAR(255),
      location_lat DECIMAL(10, 8),
      location_lng DECIMAL(11, 8),

    -- Input source tracking (supports receipt OCR, voice, smart text, etc.)
      input_source VARCHAR(20) NOT NULL DEFAULT 'manual',
      input_metadata JSONB,  -- Source-specific data (see Input Source Metadata section)

    -- Creator tracking
      created_by VARCHAR(60) NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
      created_by_guest VARCHAR(60) REFERENCES guest_users(id) ON DELETE RESTRICT,

    -- Sync support columns
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
      version INTEGER NOT NULL DEFAULT 1,

    -- Constraints
      CONSTRAINT chk_expenses_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_expenses_split_type CHECK (split_type IN ('equal', 'exact', 'percentage', 'shares')),
    CONSTRAINT chk_expenses_amount CHECK (total_amount > 0),
    CONSTRAINT chk_expenses_input_source CHECK (input_source IN ('manual', 'receipt_ocr', 'smart_text', 'voice', 'bank_import', 'recurring')),
    CONSTRAINT chk_expenses_creator CHECK (
        (created_by IS NOT NULL) OR (created_by_guest IS NOT NULL)
    )
);

CREATE INDEX idx_expenses_group_date ON expenses(group_id, expense_date DESC) WHERE NOT is_deleted;
CREATE INDEX idx_expenses_created_by ON expenses(created_by) WHERE NOT is_deleted;
CREATE INDEX idx_expenses_category ON expenses(category_id) WHERE category_id IS NOT NULL AND NOT is_deleted;
CREATE INDEX idx_expenses_updated_at ON expenses(updated_at) WHERE NOT is_deleted;  -- For sync
CREATE INDEX idx_expenses_input_source ON expenses(input_source) WHERE NOT is_deleted;  -- For analytics

-- -----------------------------------------------------------------------------
-- Expense Payers: Who paid (supports multiple payers per expense)
-- -----------------------------------------------------------------------------
CREATE TABLE expense_payers (
    id VARCHAR(60) PRIMARY KEY,
    expense_id VARCHAR(60) NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,

    -- Either a user or guest paid
    user_id VARCHAR(60) REFERENCES users(id) ON DELETE RESTRICT,
    guest_user_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE RESTRICT,

    -- Amount this person paid (in cents)
    amount_paid BIGINT NOT NULL,

    -- Sync support
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT chk_expense_payers_amount CHECK (amount_paid > 0),
    CONSTRAINT chk_expense_payers_user_or_guest CHECK (
        (user_id IS NOT NULL AND guest_user_id IS NULL) OR
        (user_id IS NULL AND guest_user_id IS NOT NULL)
        ),
    -- One payment record per user per expense
    CONSTRAINT uq_expense_payers_user UNIQUE (expense_id, user_id),
    CONSTRAINT uq_expense_payers_guest UNIQUE (expense_id, guest_user_id)
);

CREATE INDEX idx_expense_payers_expense ON expense_payers(expense_id) WHERE NOT is_deleted;
CREATE INDEX idx_expense_payers_user ON expense_payers(user_id) WHERE user_id IS NOT NULL AND NOT is_deleted;



-- -----------------------------------------------------------------------------
-- Expense Splits: How expense is divided among participants
-- -----------------------------------------------------------------------------
CREATE TABLE expense_splits (
            id VARCHAR(60) PRIMARY KEY,
            expense_id VARCHAR(60) NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,

    -- Either a user or guest owes
            user_id VARCHAR(60) REFERENCES users(id) ON DELETE RESTRICT,
            guest_user_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE RESTRICT,

    -- Split details (all stored, calculation method depends on split_type)
            split_amount BIGINT NOT NULL,  -- Final calculated amount owed (cents)
            percentage DECIMAL(5, 2),      -- For percentage splits (0.00 to 100.00)
            shares INTEGER DEFAULT 1,      -- For shares-based splits

    -- Sync support
            created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
            is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
            version INTEGER NOT NULL DEFAULT 1,

            CONSTRAINT chk_expense_splits_amount CHECK (split_amount >= 0),
            CONSTRAINT chk_expense_splits_percentage CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100)),
            CONSTRAINT chk_expense_splits_shares CHECK (shares IS NULL OR shares > 0),
            CONSTRAINT chk_expense_splits_user_or_guest CHECK (
                (user_id IS NOT NULL AND guest_user_id IS NULL) OR
                (user_id IS NULL AND guest_user_id IS NOT NULL)
                ),
-- One split record per user per expense
            CONSTRAINT uq_expense_splits_user UNIQUE (expense_id, user_id),
            CONSTRAINT uq_expense_splits_guest UNIQUE (expense_id, guest_user_id)
);

CREATE INDEX idx_expense_splits_expense ON expense_splits(expense_id) WHERE NOT is_deleted;
CREATE INDEX idx_expense_splits_user ON expense_splits(user_id) WHERE user_id IS NOT NULL AND NOT is_deleted;

-- -----------------------------------------------------------------------------
-- Settlements: Record when debts are paid
-- -----------------------------------------------------------------------------
CREATE TABLE settlements (
     id VARCHAR(60) PRIMARY KEY,
     group_id VARCHAR(60) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

-- Who paid whom
     payer_user_id VARCHAR(60) REFERENCES users(id) ON DELETE RESTRICT,
     payer_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE RESTRICT,
     payee_user_id VARCHAR(60) REFERENCES users(id) ON DELETE RESTRICT,
     payee_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE RESTRICT,

    -- Amount settled
     amount BIGINT NOT NULL,
     currency CHAR(3) NOT NULL,

    -- Payment details
     payment_method VARCHAR(50),  -- 'cash', 'venmo', 'paypal', 'bank_transfer', etc.
     payment_reference VARCHAR(255),  -- Transaction ID if applicable
     settlement_date DATE NOT NULL DEFAULT CURRENT_DATE,

     notes TEXT,

    -- Creator
     created_by VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,

    -- Sync support columns
     created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
     is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
     version INTEGER NOT NULL DEFAULT 1,

     CONSTRAINT chk_settlements_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_settlements_amount CHECK (amount > 0),
    CONSTRAINT chk_settlements_payer CHECK (
        (payer_user_id IS NOT NULL AND payer_guest_id IS NULL) OR
        (payer_user_id IS NULL AND payer_guest_id IS NOT NULL)
    ),
    CONSTRAINT chk_settlements_payee CHECK (
        (payee_user_id IS NOT NULL AND payee_guest_id IS NULL) OR
        (payee_user_id IS NULL AND payee_guest_id IS NOT NULL)
    ),
    CONSTRAINT chk_settlements_different_parties CHECK (
        payer_user_id IS DISTINCT FROM payee_user_id OR
        payer_guest_id IS DISTINCT FROM payee_guest_id
    )
);

CREATE INDEX idx_settlements_group ON settlements(group_id, settlement_date DESC) WHERE NOT is_deleted;
CREATE INDEX idx_settlements_payer_user ON settlements(payer_user_id) WHERE payer_user_id IS NOT NULL AND NOT is_deleted;
CREATE INDEX idx_settlements_payee_user ON settlements(payee_user_id) WHERE payee_user_id IS NOT NULL AND NOT is_deleted;

-- -----------------------------------------------------------------------------
-- User Balances: Denormalized balance cache for fast queries
-- -----------------------------------------------------------------------------
CREATE TABLE user_balances (
   id VARCHAR(60) PRIMARY KEY,
   group_id VARCHAR(60) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- Who owes whom (from_user owes to_user)
   from_user_id VARCHAR(60) REFERENCES users(id) ON DELETE CASCADE,
   from_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE CASCADE,
   to_user_id VARCHAR(60) REFERENCES users(id) ON DELETE CASCADE,
   to_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE CASCADE,

   currency CHAR(3) NOT NULL,

    -- Positive balance = from owes to
    -- Stored as net balance after debt simplification
   balance BIGINT NOT NULL DEFAULT 0,

    -- Last recalculation timestamp
   calculated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

   updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                               CONSTRAINT chk_user_balances_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_user_balances_from CHECK (
        (from_user_id IS NOT NULL AND from_guest_id IS NULL) OR
        (from_user_id IS NULL AND from_guest_id IS NOT NULL)
    ),
    CONSTRAINT chk_user_balances_to CHECK (
        (to_user_id IS NOT NULL AND to_guest_id IS NULL) OR
        (to_user_id IS NULL AND to_guest_id IS NOT NULL)
    ),
    CONSTRAINT chk_user_balances_different CHECK (
        from_user_id IS DISTINCT FROM to_user_id OR
        from_guest_id IS DISTINCT FROM to_guest_id
    ),
    -- Unique constraint for the balance pair
    CONSTRAINT uq_user_balances UNIQUE (group_id, from_user_id, from_guest_id, to_user_id, to_guest_id, currency)
);

CREATE INDEX idx_user_balances_group ON user_balances(group_id);
CREATE INDEX idx_user_balances_from_user ON user_balances(from_user_id) WHERE from_user_id IS NOT NULL;
CREATE INDEX idx_user_balances_to_user ON user_balances(to_user_id) WHERE to_user_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- Exchange Rates: Currency conversion cache (ECB rates)
-- -----------------------------------------------------------------------------
CREATE TABLE exchange_rates (
    id VARCHAR(60) PRIMARY KEY,
    base_currency CHAR(3) NOT NULL,
    target_currency CHAR(3) NOT NULL,
    rate DECIMAL(18, 8) NOT NULL,  -- High precision for accurate conversion
    effective_date DATE NOT NULL,

    -- Source tracking
    source VARCHAR(50) NOT NULL DEFAULT 'ECB',  -- 'ECB', 'manual', etc.

    fetched_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_exchange_rates_base CHECK (base_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_exchange_rates_target CHECK (target_currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_exchange_rates_positive CHECK (rate > 0),
    CONSTRAINT uq_exchange_rates UNIQUE (base_currency, target_currency, effective_date)
);

CREATE INDEX idx_exchange_rates_lookup ON exchange_rates(base_currency, target_currency, effective_date DESC);

-- ============================================================================
-- SUPPORTING TABLES
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Activity Log: Audit trail for sync and activity feed
-- -----------------------------------------------------------------------------
CREATE TABLE activity_log (
      id VARCHAR(60) PRIMARY KEY,
      group_id VARCHAR(60) NOT NULL REFERENCES groups(id) ON DELETE CASCADE,

    -- What happened
    activity_type VARCHAR(50) NOT NULL,

    -- References to affected entities
      expense_id VARCHAR(60) REFERENCES expenses(id) ON DELETE SET NULL,
      settlement_id VARCHAR(60) REFERENCES settlements(id) ON DELETE SET NULL,

    -- Who did it
      actor_user_id VARCHAR(60) REFERENCES users(id) ON DELETE SET NULL,
      actor_guest_id VARCHAR(60) REFERENCES guest_users(id) ON DELETE SET NULL,

    -- Activity details (JSONB for flexibility)
      details JSONB NOT NULL DEFAULT '{}'::jsonb,

      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

      CONSTRAINT chk_activity_log_type CHECK (activity_type IN (
                                                                    'expense_created', 'expense_updated', 'expense_deleted',
                                                                    'settlement_created', 'settlement_deleted',
                                                                    'member_joined', 'member_left', 'member_removed',
                                                                    'group_created', 'group_updated', 'group_archived'
              ))
);

CREATE INDEX idx_activity_log_group_time ON activity_log(group_id, created_at DESC);
CREATE INDEX idx_activity_log_expense ON activity_log(expense_id) WHERE expense_id IS NOT NULL;