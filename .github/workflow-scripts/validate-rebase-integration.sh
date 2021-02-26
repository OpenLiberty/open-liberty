#!/bin/bash

# Insure we have latest origin/integration
git fetch origin integration

# Get SHAs for commits and the log between integration -> this
INTEGRATION_SHA=$(git rev-parse --short origin/integration)
THIS_SHA=$(git rev-parse --short HEAD)
DIFF=$(git log --oneline $THIS_SHA ^$INTEGRATION_SHA)

# Output for debug
echo "Integration is at commit $INTEGRATION_SHA"
echo "This branch is at commit $THIS_SHA"

# Verify that checkout command correctly performed rebase
echo $DIFF | grep "rebase onto integration before build"
if [ $? != 0 ]; then
  echo "::error::This branch was not rebased onto integration in an expected way: $DIFF"
  exit 1;
else
  echo "This branch was rebased onto integration."
fi