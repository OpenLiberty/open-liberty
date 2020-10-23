set +e
cd dev
mkdir unit-results

for project in */ ; do
    if [[ -d "$project/build/libs/test-reports/test" ]]; then
        pushd $project/build/libs/test-reports/test &> /dev/null
        find . -name 'TEST-*.xml' -exec sh -c '
		    for name do
		        cp $name ../../../../../unit-results/
		    done' sh {} +
        popd &> /dev/null
    fi
done

testspace "[unit tests]unit-results/TEST-*.xml"