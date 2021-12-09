#!/bin/bash

##############################
# This script checks copyright headers for all files that were modified for indicators
# that we might be attempting to open source confidential code. 
# Inputs: none
# Outputs: none (just fail and report if we find any)
##############################

# Prevent script from reporting failures
set +e

# Strings indicating IBM-specific copyright notice.
# We don't want any of these tokens to be in Open Liberty.
IBM_COPYRT_STR1="IBM Confidential"
IBM_COPYRT_STR2="Property of IBM"
IBM_COPYRT_STR3="Restricted Materials of IBM"

rm copyright_modified_files.diff &> /dev/null
git diff --name-only HEAD^...HEAD^2 >> copyright_modified_files.diff

# Ensure files modified in this PR have valid copyright headers
BAD_FILES=""
while read MODIFIED_FILE; do
  # Allow this file to contain the bad tokens
  if [[ $MODIFIED_FILE == '.github/workflow-scripts/validate-copyright-headers.sh' ]]; then
    continue
  fi
  echo "Checking file $MODIFIED_FILE"
  if grep -q -e "$IBM_COPYRT_STR1" -e "$IBM_COPYRT_STR2" -e "$IBM_COPYRT_STR3" "$MODIFIED_FILE"; then
    BAD_LINES=$(grep -n -e "$IBM_COPYRT_STR1" -e "$IBM_COPYRT_STR2" -e "$IBM_COPYRT_STR3" "$MODIFIED_FILE")
    echo "::error::The modified file $MODIFIED_FILE has an invalid copyright header. Problem lines are: $BAD_LINES";
    BAD_FILES="$BAD_FILES $MODIFIED_FILE"
  fi
done < copyright_modified_files.diff

# If any files with invalid copyright headers were found, report them and fail
echo "BAD_FILES is $BAD_FILES"
if [[ $BAD_FILES != "" ]]; then
  echo "::error::The following modified files had invalid copyright headers: $BAD_FILES"
  exit 1;
else
  echo "The copyright header check completed normally."
fi
