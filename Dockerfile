FROM openjdk:17-jdk-slim-buster

ARG JAR_FILE
ENV DEBUG_PORT=8123

COPY utils/config-and-run.sh /config-and-run.sh
RUN chmod +x /config-and-run.sh
COPY ${JAR_FILE} /spark-application-event-watcher.jar

CMD ["/config-and-run.sh"]