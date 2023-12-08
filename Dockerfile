FROM ghcr.io/navikt/baseimages/temurin:21
COPY ./target/familie-ef-personhendelse.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
