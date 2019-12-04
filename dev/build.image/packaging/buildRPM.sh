#!/bin/sh

WD1=$(pwd)
cd rpmbuild 
rpmbuild -ba SPECS/openliberty.spec 2>&1
echo "rpmbuild RC:$RC"
cd $WD1
