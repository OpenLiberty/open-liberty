#!/bin/bash
# This script runs all unit tests and outputs final values [total / successes / failed / skipped]

cd dev
chmod +x gradlew

echo "Running gradle test and testReport tasks.  This will take approx. 30 minutes."

# Redirect stdout to log file that will get archived if build fails
echo "::group::Gradle Output"
./gradlew --continue cnf:initialize testResults -Dgradle.test.ignoreFailures=true
echo "::endgroup::"

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

# Copy off test results to be archived
total=$(grep tests.total.all generated.properties | sed 's/[^0-9]*//g')
successful=$(grep tests.total.successful generated.properties | sed 's/[^0-9]*//g')
failed=$(grep tests.total.failed generated.properties | sed 's/[^0-9]*//g')
skipped=$(grep tests.total.skipped generated.properties | sed 's/[^0-9]*//g')

#Setup output directory 
UNIT_DIR=$PWD/unit-results
mkdir $UNIT_DIR

#Collect test results in central location
#Check each project
for project in */ ; do
    # If unit test folder exists
    if [[ -d "$project/build/libs/test-reports/test" ]]; then
        pushd $project/build/libs/test-reports/test &> /dev/null
        # Then copy unit test file(s) to central location
        for f in TEST-com.*.xml TEST-io.*.xml TEST-wlp.*.xml ; do 
            cp $f $UNIT_DIR  &> /dev/null #ignore cases where literal globs are evaluated
        done
        popd &> /dev/null
    fi
done
echo "Unit Test results in $UNIT_DIR"

#Output total test results
echo "::group::Final Test Results"
echo "Total: $total"
echo "Successful: $successful"
echo "Failed: $failed"
echo "Skipped: $skipped"
echo "::endgroup::"

#Report failure
if [ $failed -gt 0 ]; then
    echo "::set-output name=status::failure"
else
    echo "::set-output name=status::success"
fi