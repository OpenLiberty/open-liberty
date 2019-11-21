#!/bin/sh

WD1=$(pwd)
cd rpmbuild 
rpmbuild -ba SPECS/openliberty.spec
cd $WD1
