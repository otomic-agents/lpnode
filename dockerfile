FROM openjdk:21-slim AS builder

COPY . /src/
RUN cd src && ./mvnw package 



From openjdk:21-slim

COPY --from=builder /src/target /app

RUN apk update && apk add unbound && rm -rf /var/cache/apk/*

USER root
RUN chmod -R 777 /app

# ENTRYPOINT ["java","-jar","./app/obridge-0.0.1-SNAPSHOT.jar"]
ENTRYPOINT ["sh", "-c"]
CMD ["/etc/init.d/unbound start & java -jar ./app/obridge-0.0.1-SNAPSHOT.jar"]