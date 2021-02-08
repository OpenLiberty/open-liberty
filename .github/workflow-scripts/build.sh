#!/bin/bash

#Setup gradle
cd dev
chmod +x gradlew

echo "::group::Building Liberty.  This will take approx. 30 minutes"

#Initalize, assemble, and redirect output to a gradle.log file
mkdir tmp && touch tmp/gradle.log
./gradlew cnf:initialize assemble &> tmp/gradle.log

echo "::endgroup::"

#Zip up Open Liberty artifact to be used by future jobs
cd .. && zip -rq openliberty-image.zip dev/build.image/wlp/