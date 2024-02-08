CREATE TABLE kontantstotte_vedtakhendelse (
    behandling_id VARCHAR PRIMARY KEY,
    opprettet_tid TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);
