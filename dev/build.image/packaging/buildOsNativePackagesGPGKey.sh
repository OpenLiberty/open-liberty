#!/bin/sh

#add rpm macro for rpm signing to identify key
touch .rpmmacros
echo "%_gpg_name Open Liberty Project" >> .rpmmacros

#run the expect script to build the rpm
expect -f rpmBuild.expect

#Run the expect script to build the deb
expect -f debBuild.expect