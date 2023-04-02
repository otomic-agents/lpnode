FROM openjdk:17-jdk-alpine AS builder

COPY . /src/
RUN cd src && ./mvnw package 


From openjdk:17-jdk-alpine

COPY --from=builder /src/target /app
USER root
RUN chmod -R 777 /app

ENTRYPOINT ["java","-jar","./app/obridge-0.0.1-SNAPSHOT.jar"]