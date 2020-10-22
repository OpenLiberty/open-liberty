set +e

echo "Done running all FAT buckets. Checking for failures now."

cd dev
mkdir failing_buckets
FAILURE=false

echo "### Bucket results";
for FAT_BUCKET in $FAT_BUCKETS
do
  if [[ ! -f "$FAT_BUCKET/build/libs/autoFVT/output/passed.log" ]]; then
    echo "  [!FAILED!] $FAT_BUCKET";
    FAILURE=true
    if [[ -d $FAT_BUCKET/build/libs/autoFVT ]]; then
      pushd $FAT_BUCKET/build/libs/autoFVT &> /dev/null
      zip -r ../../../../failing_buckets/$FAT_BUCKET.zip output/ results/ &> /dev/null
      popd &> /dev/null
    fi
  else
    echo "  [ PASSED ] $FAT_BUCKET";
  fi
done

set -e
if $FAILURE; then
  echo "At least one bucket failed.";
  exit 1;
fi

