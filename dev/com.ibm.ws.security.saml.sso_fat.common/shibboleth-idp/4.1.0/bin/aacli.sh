#!/usr/bin/env bash

declare LOCATION

LOCATION=$(dirname $0)

$LOCATION/runclass.sh -Dnet.shibboleth.idp.cli.arguments=net.shibboleth.idp.cli.ResolverTestArguments \
    net.shibboleth.idp.cli.CLI "$@"
