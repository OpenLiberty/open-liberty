# Params:
# FAT_BUCKETS: a comma separate list of fat bucket names
# CATEGORY:    the category name (e.g. CDI_1)

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
  
./gradlew :cnf:initialize :com.ibm.ws.componenttest:build :fattest.simplicity:build
for FAT_BUCKET in $FAT_BUCKETS
do
  echo "### BEGIN running FAT bucket $FAT_BUCKET"
  BUCKET_PASSED=true
  ./gradlew :$FAT_BUCKET:buildandrun || BUCKET_PASSED=false
  OUTPUT_DIR=$FAT_BUCKET/build/libs/autoFVT/output
  mkdir -p $OUTPUT_DIR
  if $BUCKET_PASSED; then
    echo "The bucket $FAT_BUCKET passed.";
    touch "$OUTPUT_DIR/passed.log";
  else
    echo "::error::The bucket $FAT_BUCKET failed.";
    touch "$OUTPUT_DIR/fail.log";
  fi
  echo "### END running FAT bucket $FAT_BUCKET";
done

echo "Done running all FAT buckets."

