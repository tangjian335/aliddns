FROM openjdk:8-jdk-alpine
ADD ip1-1.0-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]