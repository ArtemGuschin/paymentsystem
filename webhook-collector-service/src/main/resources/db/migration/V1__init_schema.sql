CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


CREATE TABLE payment_provider_callbacks (
                                            uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                            created_at TIMESTAMP DEFAULT now() NOT NULL,
                                            updated_at TIMESTAMP DEFAULT now() NOT NULL,
                                            body TEXT NOT NULL,
                                            provider_transaction_uid UUID NOT NULL,
                                            type VARCHAR(255) NOT NULL,
                                            provider VARCHAR(255) NOT NULL,

                                            CONSTRAINT uq_payment_provider_callback
                                                UNIQUE (provider, provider_transaction_uid, type)
);


CREATE TABLE verification_callbacks (
                                        uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                        created_at TIMESTAMP DEFAULT now() NOT NULL,
                                        modified_at TIMESTAMP DEFAULT now() NOT NULL,
                                        body TEXT NOT NULL,
                                        transaction_uid UUID NOT NULL,
                                        profile_uid UUID NOT NULL,
                                        status VARCHAR(25) NOT NULL,
                                        type VARCHAR(255) NOT NULL
);


CREATE TABLE unknown_callbacks (
                                   uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                                   created_at TIMESTAMP DEFAULT now() NOT NULL,
                                   updated_at TIMESTAMP DEFAULT now() NOT NULL,
                                   body TEXT NOT NULL
);