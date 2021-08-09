@echo off
@REM WebSphere Application Server liberty client launch script
@REM
@REM Copyright IBM Corp. 2015
@REM The source code for this program is not published or other-
@REM wise divested of its trade secrets, irrespective of what has
@REM been deposited with the U.S. Copyright Office.
@REM 
@REM ----------------------------------------------------------------------------
@REM
@REM To customize the use of this script (for example with /etc/init.d system 
@REM service managers), use the following environment variables:
@REM
@REM JAVA_HOME  - The java executable is found in %JAVA_HOME%\bin
@REM
@REM JVM_ARGS   - A list of JVM command line options,
@REM              e.g. system properties or -X parameters
@REM              The value will be expanded by cmd.exe (use quotes for spaces)
@REM
@REM LOG_DIR    - The log file directory
@REM              The default value is %WLP_CLIENT_OUTPUT_DIR%\clientName\logs
@REM
@REM LOG_FILE   - The log file name
@REM              This log file is only used if the client is run in the
@REM              background via the start action. This is not supported in client.
@REM              The default value is console.log
@REM
@REM WLP_USER_DIR - The user/custom configuration directory used to store
@REM              shared and client-specific configuration. 
@REM              See README.TXT for details about shared resource locations.
@REM              A client's configuration is at %WLP_USER_DIR%\clients\clientName
@REM              The default value is the usr directory in the install directory.
@REM
@REM WLP_CLIENT_OUTPUT_DIR - The directory containing output files for defined clients.
@REM              This directory must have both read and write permissions for
@REM              the user or users that start clients.
@REM              By default, a client's output logs and workarea are stored
@REM              in the %WLP_USER_DIR%\clients\clientName directory
@REM              (alongside configuration and applications).
@REM              If this variable is set, the output logs and workarea 
@REM              would be stored in %WLP_CLIENT_OUTPUT_DIR%\clientName.
@REM
@REM WLP_DEBUG_ADDRESS - The port to use when running the client in debug mode.
@REM              The default value is 7778.
@REM
@REM ----------------------------------------------------------------------------

setlocal enabledelayedexpansion

@REM We set enabledelayedexpansion to allow !VAR!.  Quoting rules:
@REM - Use "%VAR%" (for passing to commands) or !VAR! (for echo) to expand
@REM   variables containing unknown content.  %VAR% is substituted before
@REM   parsing, which causes problems if the value contains () or &, and it
@REM   causes the wrong value to be used if the variable is set within "if".
@REM - Use set VAR=!VAR2!, not set VAR="!VAR2!", and use "%VAR%" when testing
@REM   or passing the variable.  %0 and environment variables must be dequoted
@REM   specially to avoid issues with quotes or &.  There is no reliable way to
@REM   handle & within %*, so users should not call the script with ^&.
@REM - Use !VAR! not "%VAR%" to write to the console.  Otherwise, the quotes
@REM   will be output literally.

@REM If the user has explicitly set %errorlevel% set in their environment, then
@REM it will lose its special properties.  Reset it.
set errorlevel=

set CURRENT_DIR="%~dp0"
set CURRENT_DIR=!CURRENT_DIR:"=!
set WLP_INSTALL_DIR=!CURRENT_DIR:~0,-5!
set INVOKED="%~0"
set INVOKED=!INVOKED:"=!
set PARAMS_QUOTED=%*

@REM De-quote input environment variables.
if defined JRE_HOME set JRE_HOME=!JRE_HOME:"=!
if defined JAVA_HOME set JAVA_HOME=!JAVA_HOME:"=!
if defined LOG_DIR set LOG_DIR=!LOG_DIR:"=!
if defined LOG_FILE set LOG_FILE=!LOG_FILE:"=!
if defined WLP_USER_DIR set WLP_USER_DIR=!WLP_USER_DIR:"=!
if defined WLP_CLIENT_OUTPUT_DIR set WLP_CLIENT_OUTPUT_DIR=!WLP_CLIENT_OUTPUT_DIR:"=!
if defined WLP_DEBUG_ADDRESS set WLP_DEBUG_ADDRESS=!WLP_DEBUG_ADDRESS:"=!

@REM Consume script parameters

@REM Set the action - must be present
set ACTION=%~1
if "%ACTION%" == "" set ACTION=help:usage

@REM Set the client name - optional
set CLIENT_ARG=%2
set CLIENT_NAME=%~2
if not defined CLIENT_ARG (
  set CLIENT_NAME=defaultClient
) else if "%CLIENT_NAME%" == "" (
  set CLIENT_NAME=defaultClient
) else if "%CLIENT_NAME:~0,1%" == "-" (
  set CLIENT_NAME=defaultClient
)

@REM Set JAVA_PARAMS_QUOTED.
set JAVA_AGENT_QUOTED="-javaagent:!WLP_INSTALL_DIR!\bin\tools\ws-javaagent.jar"
if defined WLP_SKIP_BOOTSTRAP_AGENT set JAVA_AGENT_QUOTED=
set JAVA_PARAMS_QUOTED=!JVM_ARGS! -jar "%WLP_INSTALL_DIR%\bin\tools\ws-client.jar"

set RC=255

@REM process the selected option...
if "help" == "%ACTION%" (
  call:help
) else if "help:usage" == "%ACTION%" (
  call:usage
) else if "create" == "%ACTION%" (
  call:createClient
) else if "run" == "%ACTION%" (
  call:runClient
) else if "debug" == "%ACTION%" (
  if not defined WLP_DEBUG_ADDRESS set WLP_DEBUG_ADDRESS=7778
  set JAVA_PARAMS_QUOTED=-Dwas.debug.mode=true -Dcom.ibm.websphere.ras.inject.at.transform=true -Dsun.reflect.noInflation=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address="!WLP_DEBUG_ADDRESS!" !JAVA_PARAMS_QUOTED!
  call:runClient
) else if "package" == "%ACTION%" (
  call:package
) else (
  goto:actions
)

@REM
@REM THE END.
@REM -- we're really done now.
@REM EXIT /B will return to where we before calling SETLOCAL.
@REM EXIT will quit the CMD shell entirely.
@REM EXIT_ALL is required on Windows XP because it loses the errorlevel when
@REM using exit /b when this script is invoked from Java.  %COMSPEC% /c exit
@REM works but loses errorlevel when invoked from Cygwin.
@REM
if not defined EXIT_ALL set EXIT_ALL=0
if %EXIT_ALL% == 1 (
  EXIT %RC%
) else (
  EXIT /B %RC%
)
ENDLOCAL

:usage
  call:installEnv
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --help:usage
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:actions
  call:installEnv
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --help:actions:%ACTION%
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:help
  call:installEnv
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --help !CLIENT_NAME!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:createClient
  call:installEnv

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--create !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:runClient
  call:clientEnvAndJVMOptions
  if not %RC% == 0 goto:eof
  call:clientExists true
  if %RC% == 2 goto:eof

  call:clientWorkingDirectory
  !JAVA_CMD_QUOTED! !JAVA_AGENT_QUOTED! !JVM_OPTIONS! !JAVA_PARAMS_QUOTED! --batch-file !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:package
  call:clientEnv
  call:clientExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--package !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof


@REM
@REM Utility functions
@REM

@REM
@REM Set environment variables for a non-client or nonexistent client command.
@REM
:installEnv
  call:readClientEnv "%WLP_INSTALL_DIR%\etc\client.env"
  call:installEnvDefaults
  call:clientEnvDefaults
goto:eof

@REM
@REM Set variable defaults for a non-client or nonexistent client command.
@REM
:installEnvDefaults
  call:readClientEnv "%WLP_INSTALL_DIR%\java\java.env"
  call:readClientEnv "%WLP_INSTALL_DIR%\etc\default.env"

  if not defined WLP_DEFAULT_USER_DIR set WLP_DEFAULT_USER_DIR=!WLP_INSTALL_DIR!\usr
  if not defined WLP_USER_DIR set WLP_USER_DIR=!WLP_DEFAULT_USER_DIR!

  if not defined WLP_DEFAULT_CLIENT_OUTPUT_DIR set WLP_DEFAULT_CLIENT_OUTPUT_DIR=!WLP_USER_DIR!\clients
  if not defined WLP_CLIENT_OUTPUT_DIR set WLP_CLIENT_OUTPUT_DIR=!WLP_DEFAULT_CLIENT_OUTPUT_DIR!

  set CLIENT_CONFIG_DIR=!WLP_USER_DIR!\clients\!CLIENT_NAME!
goto:eof

@REM
@REM Set defaults for client variables.
@REM
:clientEnvDefaults
  set CLIENT_OUTPUT_DIR=!WLP_CLIENT_OUTPUT_DIR!\!CLIENT_NAME!

  if not defined LOG_DIR (
    set X_LOG_DIR=!CLIENT_OUTPUT_DIR!\logs
  ) else (
    set X_LOG_DIR=!LOG_DIR!
  )
  
  if not defined LOG_FILE (
    set X_LOG_FILE=console.log
  ) else (
    set X_LOG_FILE=!LOG_FILE!
  )

  @REM Unset these variables to prevent collisions with nested process invocations
  set LOG_DIR=
  set LOG_FILE=

  if NOT defined JAVA_HOME (
    if NOT defined JRE_HOME (
      if NOT defined WLP_DEFAULT_JAVA_HOME (
        @REM Use whatever java is on the path
        set JAVA_CMD_QUOTED="java"
      ) else (
        if "!WLP_DEFAULT_JAVA_HOME:~0,17!" == "@WLP_INSTALL_DIR@" (
          set WLP_DEFAULT_JAVA_HOME=!WLP_INSTALL_DIR!!WLP_DEFAULT_JAVA_HOME:~17!
        )
        set JAVA_CMD_QUOTED="!WLP_DEFAULT_JAVA_HOME!\bin\java"
      )
    ) else (
      set JAVA_CMD_QUOTED="%JRE_HOME%\bin\java"
    )
  ) else (
    if exist "%JAVA_HOME%\jre\bin\java.exe" set JAVA_HOME=!JAVA_HOME!\jre
    set JAVA_CMD_QUOTED="!JAVA_HOME!\bin\java"
  )

goto:eof

@REM
@REM Set environment variables for an existing client.
@REM
:clientEnv
  call:readClientEnv "%WLP_INSTALL_DIR%\etc\client.env"
  call:installEnvDefaults

  call:readClientEnv "%CLIENT_CONFIG_DIR%\client.env"
  call:clientEnvDefaults
goto:eof

@REM
@REM Set environment variables and JVM_OPTIONS for an existing client.
@REM
:clientEnvAndJVMOptions
  call:clientEnv

  set JVM_OPTIONS=
  
  @REM Avoid HeadlessException.
  set JVM_OPTIONS=-Djava.awt.headless=true !JVM_OPTIONS!

  set RC=0
  if exist "%CLIENT_CONFIG_DIR%\client.jvm.options" (
    call:mergeJVMOptions "%CLIENT_CONFIG_DIR%\client.jvm.options"
  )
    
  @REM If the client file is not found, check for options in the etc folder.
  if not defined USE_ETC_OPTIONS (
    set JVM_TEMP_OPTIONS=
    call:mergeJVMOptions "%WLP_INSTALL_DIR%\etc\client.jvm.options"
  )
  
  @REM If we are running on Java 9, apply Liberty's built-in java 9 options
  if exist "%JAVA_HOME%\lib\modules" (
    call:mergeJVMOptions "%WLP_INSTALL_DIR%\lib\platform\java\java9.options"
  )
  
  set JVM_OPTIONS=!JVM_OPTIONS!%JVM_TEMP_OPTIONS%
  
goto:eof

@REM
@REM Read and set variables from the quoted file %1.  Empty lines and lines
@REM beginning with the hash character ('#') are ignored.  All other lines must
@REM be be of the form: VAR=VALUE
@REM
:readClientEnv
  if not exist %1 goto:eof
  for /f "usebackq eol=# delims== tokens=1,*" %%i in (%1) do set %%i=%%j
goto:eof

@REM
@REM Merging one jvm option into the options string
@REM
:mergeJVMOptions
  set jvmoptionfile=%1
  if exist %jvmoptionfile% (
    set USE_ETC_OPTIONS=defined
    call:readJVMOptions %jvmoptionfile%
  )
goto:eof

@REM
@REM Read the contents of the quoted file %1 and append the contents to
@REM %JVM_OPTIONS%.  Empty lines and lines beginning with the hash character
@REM ('#') are ignored.  All other lines are concatenated.
@REM
:readJVMOptions
  @REM delims= is not specified, so leading whitespace is not preserved.  This
  @REM is contrary to the documentation, but we keep the current behavior for
  @REM backwards compatibility since it causes no other known issues.
  for /f "usebackq eol=# tokens=*" %%i in (%1) do (
    set JVM_OPTION="%%i"
    set JVM_OPTION=!JVM_OPTION:"=!
    set JVM_TEMP_OPTIONS=!JVM_TEMP_OPTIONS! "%%i"
  )
goto:eof

@REM
@REM Set the current working directory for the existing client.
@REM
:clientWorkingDirectory
  if not exist "%CLIENT_OUTPUT_DIR%" mkdir "%CLIENT_OUTPUT_DIR%"
  cd /d "%CLIENT_OUTPUT_DIR%"
goto:eof

@REM
@REM Check the result of a Java command.
@REM
:javaCmdResult
  if %RC% == 0 goto:eof

  if !JAVA_CMD_QUOTED! == "java" (
    @REM The command does not contain "\", so errorlevel 9009 will be reported
    @REM if the command does not exist.
    if %RC% neq 9009 goto:eof
  ) else (
    @REM The command contains "\", so errorlevel 3 will be reported.  We can't
    @REM distinguish that from our own exit codes, so check for the existence
    @REM of java.exe.
    if exist !JAVA_CMD_QUOTED!.exe goto:eof
  )

  @REM Windows prints a generic "The system cannot find the path specified.",
  @REM so echo the java command.
  echo !JAVA_CMD_QUOTED!
goto:eof

@REM
@REM clientExists: Return 0 if %CLIENT_CONFIG_DIR% exists, or is "defaultClient" 
@REM                2 if client does not exist
@REM
:clientExists
  if "%CLIENT_NAME%" == "defaultClient" (
    set RC=0
  ) else if NOT EXIST "%CLIENT_CONFIG_DIR%" (
    if "%1" == "true" (
      !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --message:info.clientNotExist "%CLIENT_NAME%"
      set RC=!errorlevel!
      if !RC! == 0 (
        set RC=2
      ) else (
        call:javaCmdResult
      )
    ) else (
      set RC=2
    )
  ) else (
    set RC=0
  )
goto:eof
