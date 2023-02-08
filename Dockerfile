FROM navikt/java:17
COPY ./target/familie-ef-personhendelse.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"