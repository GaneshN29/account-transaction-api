CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    balance NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT accounts_name_not_blank CHECK (LENGTH(TRIM(name)) > 0)
);

CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    transaction_type VARCHAR(10) NOT NULL CHECK (transaction_type IN ('CREDIT', 'DEBIT')),
    amount NUMERIC(14, 2) NOT NULL CHECK (amount > 0),
    description VARCHAR(500),
    reference_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactions_account_reference_unique UNIQUE (account_id, reference_id)
);

CREATE INDEX transactions_account_created_at_idx ON transactions (account_id, created_at DESC, id DESC);

CREATE FUNCTION reject_transaction_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Transactions are immutable';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER transactions_immutable
    BEFORE UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION reject_transaction_mutation();
