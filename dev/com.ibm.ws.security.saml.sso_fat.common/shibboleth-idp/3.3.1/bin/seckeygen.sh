#! /bin/bash

declare LOCATION

LOCATION=$0
LOCATION=${LOCATION%/*}

$LOCATION/runclass.sh net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool "$@"