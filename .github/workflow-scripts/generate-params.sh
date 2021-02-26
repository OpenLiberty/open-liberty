#!/bin/bash

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
MATRIX_RESULT=$(java .github/workflow-scripts/GenerateCategories.java .github/pull_request_body.txt)
echo "::group::MATRIX_RESULT"
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