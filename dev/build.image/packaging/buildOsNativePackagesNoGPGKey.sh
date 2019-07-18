#!/bin/sh

#building rpmbuild
cd rpmbuild && rpmbuild -ba SPECS/openliberty.spec

#building debuild
cd ../debuild/openliberty/debian && debuild -d -b -us -uc