#!/bin/bash

if [ ! -z "$1" ] && [ $1 == "compile" ]; then
	./gradlew build -x test
fi
screen -d -m -S marketplace java -jar build/libs/marketplace-1.0.jar --spring.config.location=file:/etc/openbaton/marketplace/marketplace.properties
