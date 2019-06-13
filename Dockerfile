FROM maven:3.6.0-jdk-11 as builder

ADD . /build/
WORKDIR /build/

RUN mvn -T 8 --batch-mode --fail-at-end clean package

FROM openjdk:11-jdk

COPY --from=builder /build/target/toxiproxyscheduler-1.0-SNAPSHOT.jar toxiproxyscheduler.jar
COPY java.sh /java.sh

CMD ["bash", "java.sh"]
