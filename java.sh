#/bin/bash

if [ "$DEBUG" == 'true' ];
then
    DEBUG_PARAMS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:1044"
fi

java $DEBUG_PARAMS -XX:MaxRAMPercentage=100.0 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:HeapDumpPath=/opt/ -XX:OnOutOfMemoryError="kill -9 %p" -jar /toxiproxyscheduler.jar