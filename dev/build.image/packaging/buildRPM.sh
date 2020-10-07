#!/bin/sh

WD1=$(pwd)
RPM_VER=$1
RPMFILE=${WD1}/rpmbuild/RPMS/noarch/openliberty-${RPM_VER}-1.noarch.rpm
cd rpmbuild 
rpmbuild -ba SPECS/openliberty.spec 2>&1
echo "rpmbuild RC:$RC"
cd $WD1
