#!/bin/sh

#This script takes the Open Liberty version as it's argument
#ARGUMENT1 = driver version - eg. 19.0.0.12

driverVer=$1
DEB_CHANGELOG_FILE=$(pwd)/debuild/openliberty/debian/changelog
RPM_CHANGELOG_FILE=$(pwd)/rpmbuild/SPECS/openliberty.spec
RPM_TEMPLATE=$(pwd)/openliberty.spec.template
MAINTAINER="Admin <admin@openliberty.io>"
DATE=$(date)
DEB_TIMESTAMP=$(date -d "$DATE" +"%a, %d %b %Y %T %z")
RPM_TIMESTAMP=$(date -d "$DATE" +"%a %b %d %Y")

echo "Generating .deb changelog"
#Generate .deb changelog
echo "openliberty (${driverVer}-1ubuntu1) stable; urgency=medium" > $DEB_CHANGELOG_FILE
echo "" >> $DEB_CHANGELOG_FILE
echo "  * This package contains Open Liberty ${driverVer}" >> $DEB_CHANGELOG_FILE
echo "" >> $DEB_CHANGELOG_FILE
echo " -- ${MAINTAINER}  "${DEB_TIMESTAMP} >> $DEB_CHANGELOG_FILE

echo "Generating .rpm changelog"
#generate updated RPM SPEC
cp $RPM_TEMPLATE $RPM_CHANGELOG_FILE
echo "" >> $RPM_CHANGELOG_FILE
echo "%changelog" >> $RPM_CHANGELOG_FILE
echo "* ${RPM_TIMESTAMP} "${MAINTAINER}" - ${driverVer}-1"  >> $RPM_CHANGELOG_FILE
echo "- This package contains Open Liberty ${driverVer}" >> $RPM_CHANGELOG_FILE

#update liberty version in rpm spec file
echo "updating rpm spec"
sed -i -e "s/@SPEC_VERSION@/${driverVer}/" $RPM_CHANGELOG_FILE
