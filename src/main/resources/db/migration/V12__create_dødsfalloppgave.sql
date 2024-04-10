CREATE TABLE dødsfalloppgave
(
    hendelse_id         UUID PRIMARY KEY,
    person_id           VARCHAR      NOT NULL,
    beskrivelse         VARCHAR      NOT NULL,
    personhendelsetype  VARCHAR      NOT NULL,
    endringstype        VARCHAR      NOT NULL,
    hendelsestid        TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    opprettetoppgavetid TIMESTAMP(3)          DEFAULT LOCALTIMESTAMP
);