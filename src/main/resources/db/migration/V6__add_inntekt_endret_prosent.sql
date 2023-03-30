ALTER TABLE inntektsendringer DROP COLUMN harendretinntekt;
ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_to_maaneder_tilbake INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_forrige_maaned INT DEFAULT 0;