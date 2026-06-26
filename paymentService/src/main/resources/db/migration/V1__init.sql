CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- PAYMENT PROVIDERS
-- ============================================================

CREATE TABLE payment_providers (
                                   id              SERIAL PRIMARY KEY,
                                   name            VARCHAR(50) NOT NULL UNIQUE,
                                   description     VARCHAR(256)
);

-- ============================================================
-- PAYMENT METHODS
-- ============================================================

CREATE TABLE payment_methods (
                                 id                      SERIAL PRIMARY KEY,

                                 provider_id             INTEGER NOT NULL
                                     REFERENCES payment_providers(id),

                                 type                    VARCHAR(32) NOT NULL,

                                 created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

                                 modified_at             TIMESTAMP,

                                 name                    VARCHAR(64) NOT NULL,

                                 is_active               BOOLEAN NOT NULL DEFAULT TRUE,

                                 provider_unique_id      VARCHAR(128) NOT NULL UNIQUE,

                                 provider_method_type    VARCHAR(32) NOT NULL,

                                 logo                    TEXT,

                                 profile_type            VARCHAR(24) NOT NULL DEFAULT 'INDIVIDUAL'
);

-- ============================================================
-- PAYMENT METHOD DEFINITIONS
-- ============================================================

CREATE TABLE payment_method_definitions (
                                            id                      SERIAL PRIMARY KEY,

                                            payment_method_id       INTEGER NOT NULL
                                                REFERENCES payment_methods(id),

                                            currency_code           VARCHAR(3),

                                            country_alpha3_code     VARCHAR(3),

                                            is_all_currencies       BOOLEAN NOT NULL DEFAULT FALSE,

                                            is_all_countries        BOOLEAN NOT NULL DEFAULT FALSE,

                                            is_priority             BOOLEAN NOT NULL DEFAULT FALSE,

                                            is_active               BOOLEAN NOT NULL DEFAULT TRUE,

                                            CONSTRAINT uk_payment_method_definition
                                                UNIQUE (
                                                        payment_method_id,
                                                        currency_code,
                                                        country_alpha3_code
                                                    )
);

-- ============================================================
-- PAYMENT METHOD REQUIRED FIELDS
-- ============================================================

CREATE TABLE payment_method_required_fields (
                                                uid                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

                                                created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

                                                modified_at             TIMESTAMP NOT NULL DEFAULT NOW(),

                                                payment_method_id       INTEGER NOT NULL
                                                    REFERENCES payment_methods(id),

                                                payment_type            VARCHAR(64) NOT NULL,

                                                country_alpha3_code     VARCHAR(3),

                                                name                    VARCHAR(128) NOT NULL,

                                                data_type               VARCHAR(128) NOT NULL,

                                                validation_type         VARCHAR(128),

                                                validation_rule         VARCHAR(256),

                                                default_value           VARCHAR(128),

                                                values_options          TEXT,

                                                description             VARCHAR(255),

                                                placeholder             VARCHAR(255),

                                                representation_name     VARCHAR(255),

                                                language                VARCHAR(2),

                                                is_active               BOOLEAN NOT NULL DEFAULT TRUE,

                                                CONSTRAINT uk_required_field
                                                    UNIQUE (
                                                            language,
                                                            name,
                                                            payment_method_id,
                                                            payment_type,
                                                            country_alpha3_code
                                                        )
);

-- ============================================================
-- PAYMENTS
-- ============================================================

CREATE TABLE payments (
                          id                          SERIAL PRIMARY KEY,

                          payment_method_id           INTEGER NOT NULL
                              REFERENCES payment_methods(id),

                          external_transaction_id     VARCHAR(128),

                          internal_transaction_id     VARCHAR(128),

                          amount                      NUMERIC(18,2) NOT NULL,

                          currency                    VARCHAR(3) NOT NULL,

                          status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                          created_at                  TIMESTAMP NOT NULL DEFAULT NOW(),

                          modified_at                 TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_payment_provider
    ON payment_methods(provider_id);

CREATE INDEX idx_payment_definition_method
    ON payment_method_definitions(payment_method_id);

CREATE INDEX idx_required_field_method
    ON payment_method_required_fields(payment_method_id);

CREATE INDEX idx_payment_internal_tx
    ON payments(internal_transaction_id);

CREATE INDEX idx_payment_external_tx
    ON payments(external_transaction_id);

CREATE INDEX idx_payment_status
    ON payments(status);