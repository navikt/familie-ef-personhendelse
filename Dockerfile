FROM eclipse-temurin:17 as jre-build
COPY ./target/familie-ef-personhendelse-1.0-SNAPSHOT.jar "app.jar"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"