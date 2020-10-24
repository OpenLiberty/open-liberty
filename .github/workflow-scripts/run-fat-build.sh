# Params:
# FAT_BUCKETS: a comma separate list of fat bucket names
# CATEGORY:    the category name (e.g. CDI_1)
# GH_EVENT_NAME: the event triggering this workflow. Will be 'pull_request' if a PR triggered it

set +e
cd dev
chmod +x gradlew
chmod 777 build.image/wlp/bin/*
echo "org.gradle.daemon=false" >> gradle.properties

echo "Environment dump:"
env
echo "Ant version:"
ant -v
echo "Java version:"
java -version

FAT_ARGS=""

# If this is the special 'MODIFIED_FULL_MODE' job, figure out which buckets 
# were directly modfied (if any) so they can be launched in FULL mode
if [[ "MODIFIED_FULL_MODE" == $CATEGORY ]]; then
  FAT_ARGS="-Dfat.test.mode=FULL"
  git diff --name-only HEAD^...HEAD^2 >> modified_files.diff
  echo "Modified files are:"
  cat modified_files.diff
  FAT_BUCKETS=$(sed -n "s/^dev\/\(.*_fat[^\/]*\)\/.*$/\1/p" modified_files.diff | uniq)
  if [[ -z $FAT_BUCKETS ]]; then
    echo "No FATs were directly modfied. Skipping this job."
    exit 0
  fi
fi

echo "Will be running buckets $FAT_BUCKETS"
for FAT_BUCKET in $FAT_BUCKETS
do
  if [[ ! -d "$FAT_BUCKET" ]]; then
    echo "::error::Bucket $FAT_BUCKET does not exist.";
    exit 1;
  fi
done

# For PR type events, set the git_diff for the change detector tool so unreleated FATs do not run
if [[ $GH_EVENT_NAME == 'pull_request' && "MODIFIED_FULL_MODE" != $CATEGORY ]]; then
  FAT_ARGS="-Dgit_diff=HEAD^...HEAD^2"
  echo "This event is a pull request. Will run FATs with: $FAT_ARGS"
fi
  
./gradlew :cnf:initialize :com.ibm.ws.componenttest:build :fattest.simplicity:build
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "### BEGIN running FAT bucket $FAT_BUCKET"
  BUCKET_PASSED=true
  ./gradlew :$FAT_BUCKET:buildandrun $FAT_ARGS || BUCKET_PASSED=false
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
  echo "Uploading fat results to testspace"
  testspace "[$FAT_BUCKET]$RESULTS_DIR/junit/TEST-*.xml"
  echo "### END running FAT bucket $FAT_BUCKET";
done

echo "Done running all FAT buckets."

