REM Copyright (c) 2016 IBM Corporation and others.
REM All rights reserved. This program and the accompanying materials
REM are made available under the terms of the Eclipse Public License 2.0
REM which accompanies this distribution, and is available at
REM http://www.eclipse.org/legal/epl-2.0/
REM
REM Contributors:
REM     IBM Corporation

@IF EXIST "%~dp0\node.exe" (
  "%~dp0\node.exe"  "%~dp0\..\intern\bin\intern-client.js" %*
) ELSE (
  @SETLOCAL
  @SET PATHEXT=%PATHEXT:;.JS;=;%
  node  "%~dp0\..\intern\bin\intern-client.js" %*
)