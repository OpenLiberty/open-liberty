#!/bin/sh

#This script takes the Open Liberty version as it's argument
#ARGUMENT1 = driver version - eg. 19.0.0.12
#ARGUMENT2 = isRelease build - true or false
#ARGUMENT3 = RPM Timestamp
#ARGUMENT4 = DEB Timestamp

driverVer=$1
isRelease=$2
RPM_TIMESTAMP=$3
DEB_TIMESTAMP=$4

#Unzip contents of openliberty-all.zip
unzip -qq tempPackagingDir/openliberty-*.zip -d tempPackagingDir/tempTar

#setting up the server.env
mkdir -p tempPackagingDir/tempTar/wlp/etc
touch tempPackagingDir/tempTar/wlp/etc/server.env && echo "WLP_USER_DIR=/var/lib/openliberty/usr" >> tempPackagingDir/tempTar/wlp/etc/server.env && echo "PID_DIR=/var/run/openliberty" >> tempPackagingDir/tempTar/wlp/etc/server.env

#pack contents into .tar.gz
tar -czf openliberty-$driverVer.tar.gz -C tempPackagingDir/tempTar/wlp .

#copy tar.gz to the debian and rpm dir
mv openliberty-$driverVer.tar.gz debuild
cp debuild/openliberty-$driverVer.tar.gz debuild/openliberty_$driverVer.orig.tar.gz
cp debuild/openliberty-$driverVer.tar.gz rpmbuild/SOURCES

#Update changelogs
./updateChangelogs.sh $driverVer "${RPM_TIMESTAMP}" "${DEB_TIMESTAMP}"

#Generate PassPhrase file if Passphrase is defined.
if [ -z "$GPG_PASS" ]
then
     echo "GPG PASSPHRASE is undefined.  skip signing of rpm/deb packages"
else 
     echo "creating passphrase file"
     echo "${GPG_PASS}" > $HOME/.gnupg/pp.txt
fi

#build rpm
#cd rpmbuild && rpmbuild -ba SPECS/openliberty.spec
echo "Building openliberty.rpm"
./buildRPM.sh ${driverVer}

echo "Building openliberty.deb"
#build deb
#cd ../debuild/openliberty/debian && debuild -d -b -us -uc
./buildDEB.sh ${driverVer}

echo "removing passphrase file"
rm -rf $HOME/.gnupg/pp.txt
