#!/bin/sh


#check that GPG_PASS is defined
if [ -z "$GPG_PASS" ]
then
     echo "GPG PASSPHRASE is undefined.  skip signing of rpm/deb packages"
     exit 0
else 
     #!/bin/sh

     #add rpm macro for rpm signing to identify key
     #touch .rpmmacros
     #echo "%_gpg_name Open Liberty Project" >> .rpmmacros

     #run the expect script to build the rpm
     #expect -f rpmBuild.expect

     #run commands to build the deb 
     echo "${GPG_PASS}" >> ~/.gnupg/pp.txt
     debuild -d -b -p'gpg --passphrase-file ~/.gnupg/pp.txt --batch'  -e"admin@openliberty.io"
     if gpg -k |grep admin ; then
         echo gpg key imported correctly
     else
         echo gpg key not imported correctly
     fi
     rm -f ~/.gnupg/pp.txt
fi
