@echo off
setlocal

"%~dp0\runclass.bat" -Dnet.shibboleth.idp.cli.arguments=net.shibboleth.idp.cli.ResolverTestArguments net.shibboleth.idp.cli.CLI %*
