#!/bin/sh

#This script takes the Open Liberty version as it's argument
#ARGUMENT1 = driver version - eg. 19.0.0.12
#ARGUMENT2 = RPM Timestamp - eg "Mon Nov 18 2019"
#ARGUMENT3 = DEB Timestamp - eg "Mon, 18 Nov 2019 07:46:01 -0800"

driverVer=$1
DEB_CHANGELOG_FILE=$(pwd)/debuild/openliberty/debian/changelog
RPM_CHANGELOG_FILE=$(pwd)/rpmbuild/SPECS/openliberty.spec
RPM_TEMPLATE=$(pwd)/openliberty.spec.template
MAINTAINER="Admin <admin@openliberty.io>"

#check if ARG2 & ARG3 are set - otherwise fallback to hardcoded timestamp
if [ $# -eq 3 ]; then
    echo "using date command to generate datestamps"
    RPM_TIMESTAMP="$2"
    DEB_TIMESTAMP="$3"
else
    echo "$# arguments passed in, missing either RPM or DEB timestamp"
    echo "falling back to manually set datestamps"
    DEB_TIMESTAMP="Mon, 18 Nov 2019 07:46:01 -0800"
    RPM_TIMESTAMP="Mon Nov 18 2019"
fi

echo "Generating .deb changelog using datestamp:$DEB_TIMESTAMP"
#Generate .deb changelog
echo "openliberty (${driverVer}-1ubuntu1) stable; urgency=medium" > $DEB_CHANGELOG_FILE
echo "" >> $DEB_CHANGELOG_FILE
echo "  * This package contains Open Liberty ${driverVer}" >> $DEB_CHANGELOG_FILE
echo "" >> $DEB_CHANGELOG_FILE
echo " -- ${MAINTAINER}  "${DEB_TIMESTAMP} >> $DEB_CHANGELOG_FILE

echo "Generating .rpm changelog using datestamp:$RPM_TIMESTAMP"
#generate updated RPM SPEC
cp $RPM_TEMPLATE $RPM_CHANGELOG_FILE
echo "" >> $RPM_CHANGELOG_FILE
echo "%changelog" >> $RPM_CHANGELOG_FILE
echo "* ${RPM_TIMESTAMP} "${MAINTAINER}" - ${driverVer}-1"  >> $RPM_CHANGELOG_FILE
echo "- This package contains Open Liberty ${driverVer}" >> $RPM_CHANGELOG_FILE

#update liberty version in rpm spec file
echo "updating rpm spec"
sed -i -e "s/@SPEC_VERSION@/${driverVer}/" $RPM_CHANGELOG_FILE

exit 0

