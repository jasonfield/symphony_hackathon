FROM openjdk:8

MAINTAINER jason@jasonfield.com

USER root

RUN mkdir /app
COPY build/libs/rocket-1.0-SNAPSHOT.jar /app
COPY application.properties /app
COPY git.json /app
COPY certs /app/certs

WORKDIR /app

ENV SYMPHONY_CONFIG_FILE=./application.properties
CMD java -jar rocket-1.0-SNAPSHOT.jar