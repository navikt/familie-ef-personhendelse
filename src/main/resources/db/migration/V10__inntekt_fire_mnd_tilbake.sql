ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_fire_maaneder_tilbake INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN inntekt_endret_fire_maaneder_tilbake_belop INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN feilutbetaling_fire_maaneder_tilbake INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN feilutbetaling_tre_maaneder_tilbake INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN feilutbetaling_to_maaneder_tilbake INT DEFAULT 0;
ALTER TABLE inntektsendringer ADD COLUMN feilutbetaling_forrige_maaned INT DEFAULT 0;