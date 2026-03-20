alter table payments
    add column retry_count integer not null default 0,
    add column last_retry_at timestamptz null,
    add column next_retry_at timestamptz null;

create index idx_payments_status_next_retry_at
    on payments (status, next_retry_at);
