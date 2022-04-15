FROM gradle:7.3.3-alpine
EXPOSE 12345/tcp
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon
CMD ["./gradlew", "run"]
