#!/bin/bash
#Setup directory 
UNIT_DIR=$PWD/unit-results
mkdir $UNIT_DIR

cd dev

#Check each project
for project in */ ; do
    # If unit test folder exists
    if [[ -d "$project/build/libs/test-reports/test" ]]; then
        pushd $project/build/libs/test-reports/test &> /dev/null
        # Then copy unit test file(s) to central location
        for f in TEST-*.xml
        do 
            if cp $f $UNIT_DIR &> /dev/null
            then 
                : #If copy successful do nothing
            else 
                # Otherwise, unit test task may have run, but no results were producted
                echo "No unit results for $project"
            fi
        done
        popd &> /dev/null
    fi
done

echo "Unit Test results in $UNIT_DIR"

