FROM navikt/java:17
COPY ./target/familie-ef-personhendelse-1.0-SNAPSHOT.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"