CREATE TABLE outbox_events (
                               uid UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
                               created_at TIMESTAMP DEFAULT now() NOT NULL,
                               published_at TIMESTAMP,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(255) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(25) NOT NULL DEFAULT 'NEW'
);

CREATE INDEX idx_outbox_events_status_created_at
    ON outbox_events (status, created_at);