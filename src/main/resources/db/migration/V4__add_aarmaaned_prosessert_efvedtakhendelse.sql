ALTER TABLE efvedtakhendelse ADD COLUMN aar_maaned_prosessert VARCHAR;
ALTER TABLE efvedtakhendelse ADD COLUMN versjon BIGINT;
UPDATE efvedtakhendelse SET aar_maaned_prosessert='2022-01';
UPDATE efvedtakhendelse SET versjon=1;