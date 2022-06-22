FROM adoptopenjdk/openjdk11:alpine-jre
RUN mkdir /app
COPY target/scala-2.12/book-rate-scala_2.12-0.0.1-SNAPSHOT.jar /app/application.jar
CMD ["java", "-jar", "/app/application.jar"]