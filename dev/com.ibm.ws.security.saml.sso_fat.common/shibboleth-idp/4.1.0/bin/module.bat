@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.idp.module.impl.ModuleManagerCLI --home "%~dp0\.." %*
