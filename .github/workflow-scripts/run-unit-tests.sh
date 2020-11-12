#!/bin/bash
cd dev
chmod +x gradlew

echo "Running gradle test and testReport tasks.  This will take approx. 30 minutes."

# Redirect stdout to log file that will get archived if any tests fail
mkdir gradle && touch unit.gradle.log
./gradlew --continue cnf:initialize testResults > gradle/unit.gradle.log

# Gradle testResults task will save off results in generated.properties ensure that file exists otherwise fail
if [[ ! -f "generated.properties" ]]; then
    echo "generated.properties file does not exist gradle build likely failed"
    exit 1
else 
    if [ -z $(grep tests.total.all generated.properties) ]; then
        echo "Test results not generated gradle build likely failed"
        exit 1
    fi
fi 

total=$(grep tests.total.all generated.properties | sed 's/[^0-9]*//g')
successful=$(grep tests.total.successful generated.properties | sed 's/[^0-9]*//g')
failed=$(grep tests.total.failed generated.properties | sed 's/[^0-9]*//g')
skipped=$(grep tests.total.skipped generated.properties | sed 's/[^0-9]*//g')

echo -e "\n[Final Test Results] \n total:$total successful:$successful failed:$failed skipped:$skipped"

if [ $failed -gt 0 ]; then
    echo "::set-output name=status::failure"
else
    echo "::set-output name=status::success"
fi