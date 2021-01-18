#!/bin/bash

# Params:
# CATEGORY:    the category name (e.g. CDI_1)
# GH_EVENT_NAME: the event triggering this workflow. Will be 'pull_request' if a PR triggered it

set +e

# Override default LITE mode per-bucket timeout to 45m
FAT_ARGS="-Dfattest.timeout=2700000"

# If this is the special 'MODIFIED_*_MODE' job, figure out which buckets 
# were directly modfied (if any) so they can be launched
if [[ $CATEGORY =~ MODIFIED_.*_MODE ]]; then
  if [[ $CATEGORY == 'MODIFIED_FULL_MODE' ]]; then
    echo "FAT buckets in this job will run in FULL mode"
    # Give FULL mode buckets a 2h timeout each
    FAT_ARGS="-Dfat.test.mode=FULL -Dfattest.timeout=7200000"
  fi
  git diff --name-only HEAD^...HEAD^2 >> modified_files.diff
  echo "Modified files are:"
  cat modified_files.diff
  FAT_BUCKETS=$(sed -n "s/^dev\/\([^\/]*_fat[^\/]*\)\/.*$/\1/p" modified_files.diff | uniq)
  if [[ -z $FAT_BUCKETS ]]; then
    echo "No FATs were directly modfied. Skipping this job."
    exit 0
  fi
else
  # This is a regular category defined in .github/test-categories/
  if [[ ! -f .github/test-categories/$CATEGORY ]]; then
    echo "::error::Test category [$CATEGORY] does not exist. A file containing a list of FAT buckets was not found at .github/test-categories/$CATEGORY";
    exit 1;
  fi
  FAT_BUCKETS=$(awk '!/^ *#/ && NF' .github/test-categories/$CATEGORY) #ignore comments starting with # and empty lines
fi

# Ensure all buckets we plan to run exist
echo "::group:: Will be running buckets"
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
if [[ $GH_EVENT_NAME == 'pull_request' && ! $CATEGORY =~ MODIFIED_.*_MODE ]]; then
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
mkdir -p gradle/fats/ && touch setup.gradle.log
./gradlew :cnf:initialize :com.ibm.ws.componenttest:build :fattest.simplicity:build
echo "::endgroup::"

#Run fat buckets
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "::group:: Run $FAT_BUCKET with FAT_ARGS=$FAT_ARGS"
  BUCKET_PASSED=true
  ./gradlew :$FAT_BUCKET:buildandrun $FAT_ARGS || BUCKET_PASSED=false
  OUTPUT_DIR=$FAT_BUCKET/build/libs/autoFVT/output
  RESULTS_DIR=$FAT_BUCKET/build/libs/autoFVT/results
  mkdir -p $OUTPUT_DIR
  # Create a file to mark whether a bucket failed or passed
  if $BUCKET_PASSED; then
    echo "The bucket $FAT_BUCKET passed.";
    touch "$OUTPUT_DIR/passed.log";
  else
    echo "$FAT_BUCKET failed."
    touch "$OUTPUT_DIR/fail.log";
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