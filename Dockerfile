FROM openjdk:8-jre-alpine

ADD target/scala-2.12/book-rate-scala_2.12-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java","-jar","/app.jar"]