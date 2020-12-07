#!/bin/bash
# Save off PR Summary
echo $PRBODY >> .github/pull_request_body.txt
echo "PR Summary is:"
cat .github/pull_request_body.txt

# Save off modified files
git diff --name-only HEAD^...HEAD^2 >> .github/modified_files.diff
echo "Modified files in this PR are:"
cat .github/modified_files.diff

# Generate test categories
MATRIX_RESULT=$(java .github/workflow-scripts/GenerateCategories.java .github/pull_request_body.txt)
echo "MATRIX_RESULT is $MATRIX_RESULT"

# Output results
echo "::set-output name=test-os::ubuntu-18.04"
echo "::set-output name=test-java::11"
echo "::set-output name=test-matrix::$MATRIX_RESULT"