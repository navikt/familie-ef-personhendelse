ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_tre_maaneder_tilbake_belop INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_to_maaneder_tilbake_belop INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_forrige_maaned_belop INT DEFAULT 0;