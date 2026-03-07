CREATE TABLE currencies (
                            id bigserial PRIMARY KEY,
                            created_at timestamp default now() not null,
                            modified_at timestamp,
                            code varchar(3) not null UNIQUE,
                            iso_code integer not null UNIQUE,
                            description varchar(64) not null,
                            active boolean default true,
                            symbol varchar(2)
);

CREATE TABLE rate_providers (
                                provider_code varchar(3) primary key,
                                created_at timestamp default now() not null,
                                modified_at timestamp,
                                provider_name varchar(28) not null UNIQUE,
                                description varchar(255),
                                priority integer not null,
                                active boolean default true
);

CREATE TABLE conversion_rates (
                                  id bigserial PRIMARY KEY,
                                  created_at timestamp default now() not null,
                                  modified_at timestamp,
                                  source_code varchar(3) not null references currencies(code),
                                  destination_code varchar(3) not null references currencies(code),
                                  rate_begin_time timestamp default now() not null,
                                  rate_end_time timestamp not null,
                                  rate numeric not null,
                                  provider_code varchar(3) references rate_providers
);

CREATE TABLE shedlock (
                          name varchar(64) not null primary key,
                          lock_until timestamp not null,
                          locked_at timestamp not null,
                          locked_by varchar(255) not null
);