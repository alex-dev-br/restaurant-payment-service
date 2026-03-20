create table payment_outbox (
                                id uuid primary key,
                                aggregate_id uuid not null,
                                event_type varchar(50) not null,
                                exchange_name varchar(100) not null,
                                routing_key varchar(100) not null,
                                payload text not null,
                                status varchar(20) not null,
                                created_at timestamptz not null,
                                published_at timestamptz null
);

create index idx_payment_outbox_status_created_at
    on payment_outbox (status, created_at);
