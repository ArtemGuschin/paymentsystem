-- -- Enable UUID extension
 CREATE EXTENSION IF NOT EXISTS "uuid-ossp";



-- wallet_types table
CREATE TABLE IF NOT EXISTS wallet_types (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    status VARCHAR(18) NOT NULL,
    archived_at TIMESTAMP,
    user_type VARCHAR(15),
    creator VARCHAR(255),
    modifier VARCHAR(255)
    );

-- wallets table
CREATE TABLE IF NOT EXISTS wallets (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    name VARCHAR(32) NOT NULL,
    wallet_type_uid UUID NOT NULL REFERENCES wallet_types(uid),
    user_uid UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    balance DECIMAL NOT NULL DEFAULT 0.0,
    archived_at TIMESTAMP
    );

-- transactions table
CREATE TABLE IF NOT EXISTS transactions (
    uid UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    modified_at TIMESTAMP,
    user_uid UUID NOT NULL,
    wallet_uid UUID NOT NULL REFERENCES wallets(uid),
    amount DECIMAL NOT NULL DEFAULT 0.0,
    type varchar(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    comment VARCHAR(256),
    fee DECIMAL,
    target_wallet_uid UUID,
    payment_method_id BIGINT,
    failure_reason VARCHAR(256)
    );
