#!/bin/bash

##############################
# This script looks through all tests listed under test-categories and 
# compares that to all fats that exist in open liberty (found through common naming convention of _fat)
# Inputs: none
# Outputs: none (just fail and report if we find any difference)
##############################

# This script performs validation for PRs that will fail if a fat was created/renamed/removed but not updated in test-categories
TEST_CATEGORY_DIR=$PWD/.github/test-categories/
DEV_DIR=$PWD/dev
TEMP_COMPAR_DIR=$PWD/category-comparison/ && mkdir -p $TEMP_COMPAR_DIR

#Collect existing fats in test-categories
awk 1 $TEST_CATEGORY_DIR* > $TEMP_COMPAR_DIR/expectedTmp
awk '!/^ *#/ && NF' $TEMP_COMPAR_DIR/expectedTmp >> $TEMP_COMPAR_DIR/expected # Remove any comments or whitespace from from test-category files
sort -o $TEMP_COMPAR_DIR/expected $TEMP_COMPAR_DIR/expected

#Collect actual fats that exist in file system
for dir in $DEV_DIR/*_fat*/
do 
    dirName=$(basename -- "$dir")  # Strip /home/etc/...
    echo $dirName >> $TEMP_COMPAR_DIR/actual
done
sort -o $TEMP_COMPAR_DIR/actual $TEMP_COMPAR_DIR/actual

#diff the two ignoring white space (-b) and line differences (-B)
diff -b -B $TEMP_COMPAR_DIR/expected $TEMP_COMPAR_DIR/actual > $TEMP_COMPAR_DIR/diff

comp_value=$?
if [ $comp_value -eq 1 ]
then 
    testsAdded=$(cat $TEMP_COMPAR_DIR/diff | grep ">" )
    testsRemoved=$(cat $TEMP_COMPAR_DIR/diff | grep "<" )
    if [ ! -z "$testsAdded" ]; then
        echo "::error::The following fat(s) need to be added to a category under .github/test-categories: $testsAdded"
        echo "::warning::Not all fats were run, this build should not be considered complete."
        exit 0; # Post error to annotation, but do not fail build outright
    fi

    if [ ! -z "$testsRemoved" ]; then
        echo "::error::The following fat(s) need to be removed from a category under .github/test-categories: $testsRemoved"
        exit 1; # Post error to annotation and fail build, otherwise, we will try to run a fat that does not exist.
    fi
else 
    echo "List of tests in .github/test-categories matches the list of projects with '_fat' in the name"
fi