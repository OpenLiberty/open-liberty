@echo off
setlocal

"%~dp0\runclass.bat" net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategyTool %*
