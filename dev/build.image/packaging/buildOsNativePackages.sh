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

#build rpm
echo "Building openliberty.rpm"
./buildRPM.sh ${driverVer}

#build deb
echo "Building openliberty.deb"
./buildDEB.sh ${driverVer}

#Check that openliberty .deb and .rpm were actually created
if [ -f ./debuild/*.deb -a -f ./rpmbuild/RPMS/noarch/*.rpm ]; then
   exit 0
else
   echo "There was a problem building the .rpm and/or .deb packages."

   # check for existence of openliberty.deb
   if [ -f ./debuild/*.deb ]; then
      echo "openliberty .deb was built successfully"
   else
      echo "openliberty .deb was not built successfully"
      which debuild
      if [ "$?" -ne "0" ]; then
         echo "debuild command is not installed on this system"
      fi
   fi

   # check for existence of openliberty.rpm
   if [ -f ./rpmbuild/RPMS/noarch/*.rpm  ]; then
      echo "openliberty .rpm was built successfully"
   else
      echo "openliberty .rpm was not built successfully"
      which rpmbuild
      if [ "$?" -ne "0" ]; then
         echo "rpmbuild command is not installed on this system"
	 echo
	 echo "to install the rpmbuild command on Ubuntu, run the following command:"
         echo "    sudo apt-get install -y rpm devscripts build-essential fakeroot lintian debhelper"
      fi
   fi
   exit 1
fi
