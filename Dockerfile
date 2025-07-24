FROM openjdk:21-jdk-slim

WORKDIR /app

COPY build/libs/Geminify-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xmx512m -Xms256m -Dlogging.level.root=DEBUG"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]