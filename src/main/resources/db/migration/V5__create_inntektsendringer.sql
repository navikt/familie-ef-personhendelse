CREATE TABLE inntektsendringer
(
    id               UUID PRIMARY KEY,
    person_ident     VARCHAR NOT NULL,
    harNyttVedtak    BOOLEAN,
    harEndretInntekt BOOLEAN,
    prosessert_tid   TIMESTAMP(3)
);