#!/bin/bash

if [[ -n "$REMOTE_DEBUG" ]]; then
    export DEFAULT_JAVA_OPTS=$DEFAULT_JAVA_OPTS\ -agentlib:jdwp=server=y,transport=dt_socket,address=${DEBUG_PORT},suspend=n
fi

exec java $DEFAULT_JAVA_OPTS -jar /spark-application-event-watcher.jar
