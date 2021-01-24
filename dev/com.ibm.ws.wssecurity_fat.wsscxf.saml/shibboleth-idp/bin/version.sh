#! /bin/bash

declare LOCATION

LOCATION=$0
LOCATION=${LOCATION%/*}

$LOCATION/runclass.sh net.shibboleth.idp.Version