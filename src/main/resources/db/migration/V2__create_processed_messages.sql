create table processed_messages (
                                    message_id uuid primary key,
                                    message_type varchar(100) not null,
                                    aggregate_key varchar(255) not null,
                                    processed_at timestamptz not null
);

create index idx_processed_messages_aggregate_key
    on processed_messages (aggregate_key);