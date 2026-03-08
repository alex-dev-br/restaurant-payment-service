create table payments (
                          id uuid primary key,
                          order_id uuid not null,
                          client_id uuid not null,
                          status varchar(30) not null,
                          amount numeric(19,2) not null,
                          created_at timestamptz not null,
                          updated_at timestamptz not null
);

create unique index uk_payments_order_id on payments(order_id);

create index idx_payments_client_id on payments(client_id);