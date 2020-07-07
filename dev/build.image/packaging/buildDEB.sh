#!/bin/sh
WD1=$(pwd)

echo "Building .deb"
cd debuild/openliberty
debuild -d -b -us -uc
RC=$?
echo "Build .deb RC:$RC"

#CD back to previous dir
cd $WD1
