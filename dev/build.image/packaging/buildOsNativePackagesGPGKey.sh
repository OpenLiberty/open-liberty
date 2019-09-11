#!/bin/sh


#check that GPG_PASS is defined
if [ -z "$GPG_PASS" ]
then
     echo "GPG PASSPHRASE is undefined.  skip signing of rpm/deb packages"
     exit 0
else 
     File=/usr/bin/expect
     if [ -e $File ]; then
         echo "/usr/bin/expect exist"
         #!/bin/sh

         #add rpm macro for rpm signing to identify key
         touch .rpmmacros
         echo "%_gpg_name Open Liberty Project" >> .rpmmacros

         #run the expect script to build the rpm
         expect -f rpmBuild.expect

         #Run the expect script to build the deb
         expect -f debBuild.expect
     else 
         echo "/usr/bin/expect does not exist"
	 exit 0
     fi
fi
