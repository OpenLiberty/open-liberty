#!/bin/bash

##############################
# This script builds open liberty
# Inputs: none
# Outputs: 
#   File: /openliberty-image.zip    - Open Liberty build image
#   File: /dev/tmp/gradle.log       - Gradle output to be uploaded if there is a failure
##############################

#Setup gradle
cd dev
chmod +x gradlew

# global variables
OUTPUT_FILE=tmp/gradle.log && mkdir -p -- "$(dirname -- "$OUTPUT_FILE")" && touch -- "$OUTPUT_FILE"

echo "::group::Building Liberty.  This will take approx. 30 minutes"

#Initalize, assemble, and redirect output to a gradle.log file
./gradlew cnf:initialize assemble &> $OUTPUT_FILE

echo "::endgroup::"

#Zip up Open Liberty artifact to be used by future jobs
cd .. && zip -rq openliberty-image.zip dev/build.image/wlp/