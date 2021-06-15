@echo off
setlocal

set NO_PLUGIN_WEBAPP="TRUE"

"%~dp0\runclass.bat" net.shibboleth.idp.installer.plugin.impl.PluginInstallerCLI --home "%~dp0\.." %*
