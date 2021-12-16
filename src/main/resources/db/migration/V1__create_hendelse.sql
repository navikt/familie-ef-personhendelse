CREATE TABLE hendelse
(
    hendelse_id   UUID PRIMARY KEY,
    oppgave_id    BIGINT       NOT NULL,
    endringstype  VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);
