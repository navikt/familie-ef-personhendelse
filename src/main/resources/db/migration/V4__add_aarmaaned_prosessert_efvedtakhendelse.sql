ALTER TABLE efvedtakhendelse ADD COLUMN aar_maaned_prosessert VARCHAR;
ALTER TABLE efvedtakhendelse ADD COLUMN versjon BIGINT;
UPDATE efvedtakhendelse SET aar_maaned_prosessert='01-2022';
UPDATE efvedtakhendelse SET versjon=1;