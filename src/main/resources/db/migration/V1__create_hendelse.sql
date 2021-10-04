CREATE TABLE hendelse (
    hendelse_id UUID PRIMARY KEY,
    oppgave_id BIGINT,
    endringstype VARCHAR,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP
);
