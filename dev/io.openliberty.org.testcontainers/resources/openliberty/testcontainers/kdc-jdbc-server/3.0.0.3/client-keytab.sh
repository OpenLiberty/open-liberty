#!/bin/sh

# A script that takes a user name as input
# Then creates a keytab file for that user and outputs the file
# to the location /tmp/client_<user>_krb5.keytab

if [ "$#" -ne 1 ]; then
  echo "Usage: $0 user"
  exit 1 # Exit with an error status
fi

PRINCIPAL="$1@${KRB5_REALM}"
LOCATION="/tmp/client_$1_krb5.keytab"

echo "(*) Add ${PRINCIPAL} to ${LOCATION}"
printf "add_entry -password -p ${PRINCIPAL} -k 1 -e aes256-cts\npassword\nwkt ${LOCATION}" | ktutil

echo "(*) List ${LOCATION}"
klist -k -t ${LOCATION}
