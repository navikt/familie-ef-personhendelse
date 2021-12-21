CREATE TABLE efvedtakhendelse
(
    behandling_id BIGINT PRIMARY KEY,
    person_ident  VARCHAR NOT NULL,
    stonadstype   VARCHAR NOT NULL
);

alter table efvedtakhendelse alter column behandling_id type bigint using behandling_id::bigint;