-- Evently initial schema.
-- Column types are chosen to match the JPA entities so `ddl-auto=validate` passes:
--   Instant        -> timestamptz
--   LocalDateTime  -> timestamp (no time zone)
--   UUID           -> uuid
--   BigDecimal     -> numeric(10,2)
--   @Version Long  -> bigint

create table users (
    id            uuid          primary key,
    email         varchar(255)  not null unique,
    name          varchar(255)  not null,
    password_hash varchar(255)  not null,
    created_at    timestamptz   not null,
    updated_at    timestamptz   not null
);

create table user_roles (
    user_id uuid        not null references users (id) on delete cascade,
    role    varchar(32) not null,
    primary key (user_id, role)
);

create table refresh_tokens (
    id         uuid         primary key,
    user_id    uuid         not null references users (id) on delete cascade,
    token_hash varchar(64)  not null unique,
    family     uuid         not null,
    parent_id  uuid,
    expires_at timestamptz  not null,
    revoked_at timestamptz,
    user_agent varchar(512),
    ip         varchar(45),
    created_at timestamptz  not null
);
create index idx_refresh_tokens_family on refresh_tokens (family);
create index idx_refresh_tokens_user on refresh_tokens (user_id);

create table events (
    id             uuid         primary key,
    name           varchar(255) not null,
    start_datetime timestamp,
    end_datetime   timestamp,
    venue          varchar(255) not null,
    sales_start    timestamp,
    sales_end      timestamp,
    status         varchar(32)  not null,
    organizer_id   uuid         not null references users (id),
    version        bigint       not null,
    created_at     timestamptz  not null,
    updated_at     timestamptz  not null
);
create index idx_events_organizer on events (organizer_id);
create index idx_events_status on events (status);

create table ticket_types (
    id              uuid          primary key,
    name            varchar(255)  not null,
    price           numeric(10, 2) not null,
    description     varchar(1000),
    total_available integer,
    event_id        uuid          not null references events (id) on delete cascade,
    version         bigint        not null,
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null
);
create index idx_ticket_types_event on ticket_types (event_id);

create table tickets (
    id             uuid        primary key,
    status         varchar(32) not null,
    ticket_type_id uuid        not null references ticket_types (id),
    purchaser_id   uuid        not null references users (id),
    created_at     timestamptz not null,
    updated_at     timestamptz not null
);
create index idx_tickets_ticket_type on tickets (ticket_type_id);
create index idx_tickets_purchaser on tickets (purchaser_id);

create table qr_codes (
    id           uuid          primary key,
    value        varchar(2048) not null,
    status       varchar(32)   not null,
    ticket_id    uuid          not null unique references tickets (id) on delete cascade,
    generated_at timestamptz   not null
);

create table ticket_validations (
    id                  uuid        primary key,
    status              varchar(32) not null,
    method              varchar(32) not null,
    ticket_id           uuid        not null references tickets (id),
    validated_by        uuid        references users (id),
    validation_datetime timestamptz not null
);
create index idx_ticket_validations_ticket on ticket_validations (ticket_id);

create table idempotency_records (
    id              uuid         primary key,
    user_id         uuid         not null,
    idempotency_key varchar(255) not null,
    ticket_id       uuid,
    created_at      timestamptz  not null,
    constraint uk_idempotency_user_key unique (user_id, idempotency_key)
);
