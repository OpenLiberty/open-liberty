#!/bin/sh


#check that GPG_PASS is defined
if [ -z "$GPG_PASS" ]
then
      echo "GPG PASSPHRASE is undefined.  skip signing of rpm/deb packages"
      exit 0
else
      echo "GPG PASSPHRASE is defined."

      #add rpm macro for rpm signing to identify key
      touch .rpmmacros
      echo "%_gpg_name Open Liberty Project" >> .rpmmacros

      #run the expect script to build the rpm
      echo -e “${GPG_PASS}\n" | rpmbuild -ba --sign rpmbuild/SPECS/openliberty.spec

      #Run the expect script to build the deb
      cd debuild/openliberty/debian
      echo -e “${GPG_PASS}\n${GPG_PASS}\n" | debuild -d -b -sa
fi
