@echo off
setlocal

"%~dp0\runclass.bat" -Dnet.shibboleth.idp.cli.arguments=net.shibboleth.idp.cli.MetadataQueryArguments net.shibboleth.idp.cli.CLI %*
