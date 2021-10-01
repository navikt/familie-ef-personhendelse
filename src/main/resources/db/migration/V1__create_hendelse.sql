CREATE TABLE hendelse (
    hendelse_id UUID NOT NULL,
    oppgave_id BIGINT,
    endringstype VARCHAR,
    opprettet_tid TIMESTAMP(3) DEFAULT LOCALTIMESTAMP
);
