#!/bin/bash

##############################
# This scripts runs each fat in a category
# Inputs:
#   Env Var: CATEGORY                         - The category name (e.g. CDI_1)
#   Env Var: GH_EVENT_NAME                    - The event that triggered the workflow (e.g. pull_request)
# Outputs:
#   File: {autoFVT}/output/[passed/fail].log  - Indicator file used by validate-fat-failures.sh
#   File: {AutoFVT}/output/gradle.log         - Gradle output saved to a file for debugging
#   File: /dev/fat-results/*                  - junit.xml files saved in a common directory used by KyleAure/junit-report-annotations-action
##############################

# Prevent script from reporting failures
set +e

# Override default LITE mode per-bucket timeout to 45m
FAT_ARGS="-Dfattest.timeout=2700000"

# If this is the special 'MODIFIED_FULL_MODE' job then change the FAT_ARGS variable
if [[ $CATEGORY =~ MODIFIED_FULL_MODE* ]]; then
    echo "FAT buckets in this job will run in FULL mode"
    # Give FULL mode buckets a 2h timeout each
    FAT_ARGS="-Dfat.test.mode=FULL -Dfattest.timeout=7200000"
fi 

# This is a regular category defined in .github/test-categories/
if [[ ! -f .github/test-categories/$CATEGORY ]]; then
  echo "::error::Test category [$CATEGORY] does not exist. A file containing a list of FAT buckets was not found at .github/test-categories/$CATEGORY";
  exit 1;
fi

FAT_BUCKETS=$(awk '!/^ *#/ && NF' .github/test-categories/$CATEGORY) #ignore comments starting with # and empty lines

# Ensure all buckets we plan to run exist
echo "::group::Will be running buckets"
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "$FAT_BUCKET"
  if [[ ! -d "dev/$FAT_BUCKET" ]]; then
    echo "::error::FAT bucket [$FAT_BUCKET] does not exist.";
    exit 1;
  fi
done
echo "::endgroup::"

# For PR type events, set the git_diff for the change detector tool so unreleated FATs do not run
if [[ $GH_EVENT_NAME == 'pull_request' && ! $CATEGORY =~ MODIFIED_* ]]; then
  FAT_ARGS="$FAT_ARGS -Dgit_diff=HEAD^...HEAD^2"
  echo "This event is a pull request. Will run FATs with: $FAT_ARGS"
fi

# Log environment
echo "::group::Environment Dump"
env
echo "::endgroup::"
echo "::group::Java version"
java -version
echo "::endgroup::"
echo -e "::group::Ant version"
ant -version
echo "::endgroup::"

#Unzip and setup openliberty image
unzip -q openliberty-image.zip
cd dev
chmod +x gradlew
chmod 777 build.image/wlp/bin/*
echo "org.gradle.daemon=false" >> gradle.properties

#Build test infrastructure
echo "::group::Build com.ibm.ws.componenttest and fattest.simplicity"
./gradlew :cnf:initialize :com.ibm.ws.componenttest:build :fattest.simplicity:build
echo "::endgroup::"

#Run fat buckets
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "::group:: Run $FAT_BUCKET with FAT_ARGS=$FAT_ARGS"
  BUCKET_PASSED=true
  OUTPUT_FILE=tmp/$FAT_BUCKET/gradle.log && mkdir -p -- "$(dirname -- "$OUTPUT_FILE")" && touch -- "$OUTPUT_FILE"

  #Run fat
  ./gradlew :$FAT_BUCKET:buildandrun $FAT_ARGS &> $OUTPUT_FILE || BUCKET_PASSED=false

  OUTPUT_DIR=$FAT_BUCKET/build/libs/autoFVT/output
  RESULTS_DIR=$FAT_BUCKET/build/libs/autoFVT/results

  # Create a file to mark whether a bucket failed or passed
  if $BUCKET_PASSED; then
    echo "The bucket $FAT_BUCKET passed.";
    touch "$OUTPUT_DIR/passed.log";
    rm -rf tmp/
  else
    echo "$FAT_BUCKET failed."
    touch "$OUTPUT_DIR/fail.log";
    mv $OUTPUT_FILE $OUTPUT_DIR
  fi
  
  # Collect all junit files in a central location
  FAT_RESULTS=$PWD/fat-results
  mkdir $FAT_RESULTS
  echo "Collecing fat results in $FAT_RESULTS"
  for f in $RESULTS_DIR/junit/TEST-com.*.xml $RESULTS_DIR/junit/TEST-io.*.xml $RESULTS_DIR/junit/TEST-wlp.*.xml; do 
      cp $f $FAT_RESULTS &> /dev/null #ignore cases where literal globs are evaluated
  done
  echo "::endgroup::"
done

echo "Done running all FAT buckets."