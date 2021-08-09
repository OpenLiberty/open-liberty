#!/usr/bin/env bash

declare LOCATION
declare NO_PLUGIN_WEBAPP

LOCATION=$(dirname $0)
NO_PLUGIN_WEBAPP="TRUE"
export NO_PLUGIN_WEBAPP

$LOCATION/runclass.sh net.shibboleth.idp.installer.plugin.impl.PluginInstallerCLI --home "$LOCATION/.." "$@"
