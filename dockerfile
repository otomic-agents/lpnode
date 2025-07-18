FROM openjdk:21-slim AS builder

COPY . /src/
RUN cd src && ./mvnw package 



FROM openjdk:21-slim

COPY --from=builder /src/target /app

USER root
RUN chmod -R 777 /app

# ENTRYPOINT ["java","-jar","./app/obridge-0.0.1-SNAPSHOT.jar"]
ENTRYPOINT ["sh", "-c"]
CMD ["java -jar ./app/obridge-0.0.1-SNAPSHOT.jar"]