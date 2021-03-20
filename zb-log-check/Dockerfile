FROM maven:3.6.0-jdk-11 as builder
ARG VERSION=0.1.0-SNAPSHOT

COPY ./src /usr/local/src/zb-log-check/src
COPY ./pom.xml /usr/local/src/zb-log-check/pom.xml
WORKDIR /usr/local/src/zb-log-check

RUN mvn clean package -DskipTests

# Final image will only contain the built JAR and the configuration file and some UNIX utilities
FROM adoptopenjdk/openjdk11:jre-11.0.6_10-alpine as application
ARG VERSION=0.1.0-SNAPSHOT
ARG WORKDIR=/usr/local/zb-log-check
ARG DATA_DIR=${WORKDIR}/zeebe

COPY --from=builder /usr/local/src/zb-log-check/target/zb-log-check-${VERSION}-jar-with-dependencies.jar ${WORKDIR}/zb-log-check.jar

ENV WORKDIR ${WORKDIR}
ENV ZB_LOG_CHECK_DATA_DIR ${DATA_DIR}
RUN mkdir -p "$WORKDIR"
WORKDIR ${WORKDIR}

VOLUME ${DATA_DIR}
ENTRYPOINT java -jar zb-log-check.jar ${ZB_LOG_CHECK_DATA_DIR:-$(pwd)/zeebe}