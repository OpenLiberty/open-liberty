

TEST_CATEGORY_DIR=$PWD/.github/test-categories/
DEV_DIR=$PWD/dev
TEMP_COMPAR_DIR=$PWD/category-comparison/

if [ ! -d $TEMP_COMPAR_DIR ]; then 
    mkdir $TEMP_COMPAR_DIR
    touch $TEMP_COMPAR_DIR/expected
    touch $TEMP_COMPAR_DIR/actual
fi

#Collect existing fats in test-categories
for f in $TEST_CATEGORY_DIR/*
do
    cat $f >> $TEMP_COMPAR_DIR/expected
done
sort -o $TEMP_COMPAR_DIR/expected $TEMP_COMPAR_DIR/expected

#Collect actual fats that exist in file system
for dir in $DEV_DIR/*_fat*/
do 
    fname=$(basename -- "$dir")  # Strip /home/etc/...
    echo $fname >> $TEMP_COMPAR_DIR/actual
done
sort -o $TEMP_COMPAR_DIR/actual $TEMP_COMPAR_DIR/actual

#diff the two
diff -b -B $TEMP_COMPAR_DIR/expected $TEMP_COMPAR_DIR/actual > $TEMP_COMPAR_DIR/diff

comp_value=$?
if [ $comp_value -eq 1 ]
then 
    testsAdded=$(cat $TEMP_COMPAR_DIR/diff | grep ">" )
    testsRemoved=$(cat $TEMP_COMPAR_DIR/diff | grep "<" )
    if [ ! -z "$testsAdded" ]; then
        echo "::error::The following fats need to be added to a category: $testsAdded"
    fi

    if [ ! -z "$testsRemoved" ]; then
        echo "::error::The following fats need to be removed from a category: $testsRemoved"
    fi
    
    exit 1;
else 
    echo "Validate new tests resulted in the same list of fat tests."
fi