#!/bin/sh
WD1=$(pwd)
PASSPHRASE_FILE=$HOME/.gnupg/pp.txt
EMAIL=admin@openliberty.io
BUILD_SIGNED=1
KEY_FOUND=false

#Verify gpg key was installed correctly
if (gpg -k |grep $EMAIL) ; then
	echo "GPG Key found"
	KEY_FOUND=true
else
	echo "GPG Key not found"
	KEY_FOUND=false
fi

#Check for passphrase file and gpg key existence 
if  [ -e ${PASSPHRASE_FILE} ] && [ KEY_FOUND=="true" ] ;
then
	echo "Passphrase file exists.  Building signed .deb"
	cd debuild/openliberty
	debuild -d -b -p"gpg --passphrase-file $PASSPHRASE_FILE --batch"  -e"$EMAIL"
	RC=$?
	echo "Built signed .deb RC:$RC"
	BUILD_SIGNED=$RC
	cd $WD1
fi

#Build .deb without passphrase (building with passphrase failed, or GPG key or passphrase were not found)
if [ "$BUILD_SIGNED" -ne "0" ]
then
	echo "Building unsigned .deb"
	cd debuild/openliberty
  	debuild -d -b -us -uc
	RC=$?
	echo "Build unsigned .deb RC:$RC"
fi

#CD back to previous dir
cd $WD1
