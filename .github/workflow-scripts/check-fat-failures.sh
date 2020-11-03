set +e

echo "Done running all FAT buckets. Checking for failures now."

# If this is the special 'MODIFIED_*_MODE' job, figure out which buckets
# were directly modfied so we can check for their results
if [[ $CATEGORY =~ MODIFIED_.*_MODE ]]; then
  git diff --name-only HEAD^...HEAD^2 >> modified_files.diff
  echo "Modified files are:"
  cat modified_files.diff
  FAT_BUCKETS=$(sed -n "s/^dev\/\(.*_fat[^\/]*\)\/.*$/\1/p" modified_files.diff | uniq)
  if [[ -z $FAT_BUCKETS ]]; then
    echo "No FATs were directly modfied. Skipping this job."
    exit 0
  fi
else
  # This is a regular category defined in .github/test-categories/
  if [[ ! -f .github/test-categories/$CATEGORY ]]; then
    echo "::error::Test category $CATEGORY does not exist. A file containing a list of FAT buckets was not found at .github/test-categories/$CATEGORY";
    exit 1;
  fi
  FAT_BUCKETS=$(cat .github/test-categories/$CATEGORY)
fi

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

