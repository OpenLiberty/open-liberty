# Params:
# FAT_BUCKETS: a comma separate list of fat bucket names
# CATEGORY:    the category name (e.g. CDI_1)
# GH_EVENT_NAME: the event triggering this workflow. Will be 'pull_request' if a PR triggered it

set +e
cd dev
chmod +x gradlew
chmod 777 build.image/wlp/bin/*
echo "org.gradle.daemon=false" >> gradle.properties

echo "Will be running buckets $FAT_BUCKETS"
for FAT_BUCKET in $FAT_BUCKETS
do
  if [[ ! -d "$FAT_BUCKET" ]]; then
    echo "::error::Bucket $FAT_BUCKET does not exist.";
    exit 1;
  fi
done

GIT_DIFF=""
# For PR type events, set the git_diff for the change detector tool so unreleated FATs do not run
if [[ $GH_EVENT_NAME == 'pull_request' ]]; then
  GIT_DIFF="-Dgit_diff=HEAD^...HEAD^2"
  echo "This event is a pull request. Will run FATs with: $GIT_DIFF"
fi
  
./gradlew :cnf:initialize :com.ibm.ws.componenttest:build :fattest.simplicity:build
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "### BEGIN running FAT bucket $FAT_BUCKET"
  BUCKET_PASSED=true
  ./gradlew :$FAT_BUCKET:buildandrun $GIT_DIFF || BUCKET_PASSED=false
  OUTPUT_DIR=$FAT_BUCKET/build/libs/autoFVT/output
  RESULTS_DIR=$FAT_BUCKET/build/libs/autoFVT/results
  mkdir -p $OUTPUT_DIR
  if $BUCKET_PASSED; then
    echo "The bucket $FAT_BUCKET passed.";
    touch "$OUTPUT_DIR/passed.log";
  else
    echo "::error::The bucket $FAT_BUCKET failed.";
    touch "$OUTPUT_DIR/fail.log";
  fi
  echo "@@@ Uploading fat results to testspace @@@"
  testspace "[$FAT_BUCKET]$RESULTS_DIR/junit/TEST-*.xml"
  echo "### END running FAT bucket $FAT_BUCKET";
done

echo "Done running all FAT buckets."

