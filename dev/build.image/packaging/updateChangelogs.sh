#!/bin/sh

#This script takes the Open Liberty version as it's argument
#ARGUMENT1 = driver version - eg. 19.0.0.12

driverVer=$1
DEB_CHANGELOG_FILE=$(pwd)/debuild/openliberty/debian/changelog
RPM_CHANGELOG_FILE=$(pwd)/rpmbuild/SPECS/openliberty.spec
RPM_TEMPLATE=$(pwd)/openliberty.spec.template
MAINTAINER="Admin <admin@openliberty.io>"
#check date command can be run successfully
date
RC=$?
if [ "$RC" -eq "0" ] ; then
    echo "using date command to generate datestamps"
    DATE=$(date)
    DEB_TIMESTAMP=$(date -d "$DATE" +"%a, %d %b %Y %T %z")
    RPM_TIMESTAMP=$(date -d "$DATE" +"%a %b %d %Y")
else
    echo "date command executed with RC:$RC"
    echo "falling back to manually set datestamps"
    which date
    ls -l /usr/bin/date
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
