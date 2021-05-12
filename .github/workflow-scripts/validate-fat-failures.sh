#!/bin/bash

##############################
# This script looks through each fat in a category and if it failed, it will be archived for developer to debug
# Inputs:
#   Env Var: CATEGORY             - The category name (e.g. CDI_1)
# Outputs: 
#   Build Output: status          - Reports build status [failure/success]
#   Files: dev/failing_buckets/*  - Collection of failing bucket zips to be archived for debugging
##############################

# Prevent script from reporting failures
set +e

echo "Done running all FAT buckets. Checking for failures now."

# This is a regular category defined in .github/test-categories/
if [[ ! -f .github/test-categories/$CATEGORY ]]; then
  echo "::error::Test category $CATEGORY does not exist. A file containing a list of FAT buckets was not found at .github/test-categories/$CATEGORY";
  exit 1;
fi
FAT_BUCKETS=$(awk '!/^ *#/ && NF' .github/test-categories/$CATEGORY) #ignore comments starting with # and empty lines

cd dev
mkdir failing_buckets

echo "::group::Bucket results";
for FAT_BUCKET in $FAT_BUCKETS
do
  if [[ ! -f "$FAT_BUCKET/build/libs/autoFVT/output/passed.log" ]]; then
    echo "  [!FAILED!] $FAT_BUCKET";
    if [[ -d $FAT_BUCKET/build/libs/autoFVT ]]; then
      pushd $FAT_BUCKET/build/libs/autoFVT &> /dev/null
      zip -r ../../../../failing_buckets/$FAT_BUCKET.zip output/ results/ &> /dev/null
      popd &> /dev/null
    fi
    echo "::set-output name=status::failure"
  else
    echo "  [ PASSED ] $FAT_BUCKET";
    echo "::set-output name=status::success"
  fi
done
echo "::endgroup::";

