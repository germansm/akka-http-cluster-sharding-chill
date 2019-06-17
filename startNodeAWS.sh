#!/usr/bin/env bash
export IP_ADDRESS=$(ifconfig eth0 | grep "inet addr" | cut -d ':' -f 2 | cut -d ' ' -f 1)
CONFIG_FILE="application.conf"
ASSEMBLY_JAR="MSPEngine-assembly-1.0.0.jar"
JAVA_OPTS="-server -XX:+UseNUMA -XX:+UseCondCardMark -XX:-UseBiasedLocking -Xms128M -Xmx128M -Xss1M -XX:+UseParallelGC -Dfile.encoding=UTF-8 -Djava.library.path=./target/native"
COMMAND="$JAVA_OPTS -Dconfig.file=$CONFIG_FILE -jar $ASSEMBLY_JAR $IP_ADDRESS 8080"
echo "Starting the MSPEngine using:\n[\njava $COMMAND]\n]\n"
java $COMMAND