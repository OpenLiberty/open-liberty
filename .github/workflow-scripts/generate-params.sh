#!/bin/bash

##############################
# This script analyses the pull request and determines what fat
# suites need to run, and at what level (LITE/FULL).
# Inputs: 
#   Env Var: PRBODY                     - text in the PR's description field
# Outputs: 
#   Build Output: modified-categories   - reports if modified categories exit
#   Build Output: test-os               - sets os tests will run on
#   Build Output: test-java             - sets java level tests will run on
#   Build Output: test-matrix           - sets the category matrix of fats to run
# Callouts: 
#   App: GenerateCategories.java        - calculates category matrix and outputs in json format
##############################

# global variables
OUTPUT_FILE=tmp/GenerateCategories.out && mkdir -p -- "$(dirname -- "$OUTPUT_FILE")" && touch -- "$OUTPUT_FILE"

# Save off PR Summary
echo $PRBODY >> .github/pull_request_body.txt
echo "::group::PR Summary"
cat .github/pull_request_body.txt
echo "::endgroup::"

# Save off modified files
git diff --name-only HEAD^...HEAD^2 >> .github/modified_files.diff
echo "::group::Modified files in this PR"
cat .github/modified_files.diff
echo "::endgroup::"

# Generate test categories
echo "::group::GenerateCategories Debug"
java .github/workflow-scripts/GenerateCategories.java .github/pull_request_body.txt 1>> $OUTPUT_FILE
echo "::endgroup::"

echo "::group::MATRIX_RESULT"
MATRIX_RESULT=$(cat $OUTPUT_FILE)
echo $MATRIX_RESULT
echo "::endgroup::"

# Report if we have modified files or not
if ls .github/test-categories/MODIFIED_* &> /dev/null ; then
    echo "Modified test-categories found $(ls .github/test-categories/MODIFIED_*)"
    echo "::set-output name=modified-categories::true"
else
    echo "No modified test-categories found"
    echo "::set-output name=modified-categories::false"
fi

# Output results
echo "::set-output name=test-os::ubuntu-18.04"
echo "::set-output name=test-java::11"
echo "::set-output name=test-matrix::$MATRIX_RESULT"