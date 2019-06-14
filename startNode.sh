#!/usr/bin/env bash
CONFIG_FILE="src/main/resources/application.conf"
ASSEMBLY_JAR="target/scala-2.12/MSPEngine-assembly-1.0.0.jar"
JAVA_OPTS="-server -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms128M -Xmx128M -Xss1M -XX:+UseParallelGC -Dfile.encoding=UTF-8"
COMMAND="$JAVA_OPTS -Dconfig.file=$CONFIG_FILE -jar $ASSEMBLY_JAR $1"
echo "Starting the MSPEngine using:\n[\njava $COMMAND]\n]\n"
java $COMMAND
