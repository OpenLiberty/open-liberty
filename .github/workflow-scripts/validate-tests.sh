#!/bin/bash
# This script performs validation for PRs that will fail if a fat was created/renamed/removed but not updated in test-categories
TEST_CATEGORY_DIR=$PWD/.github/test-categories/
DEV_DIR=$PWD/dev
TEMP_COMPAR_DIR=$PWD/category-comparison/

if [ ! -d $TEMP_COMPAR_DIR ]; then 
    mkdir $TEMP_COMPAR_DIR
fi

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
    fi

    if [ ! -z "$testsRemoved" ]; then
        echo "::error::The following fat(s) need to be removed from a category under .github/test-categories: $testsRemoved"
    fi
    
    exit 1;
else 
    echo "List of tests in .github/test-categories matches the list of projects with '_fat' in the name"
fi