@echo off
@REM WebSphere Application Server liberty launch script
@REM
@REM Copyright IBM Corp. 2011, 2019
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
@REM              The default value is %WLP_OUTPUT_DIR%\serverName\logs
@REM
@REM LOG_FILE   - The log file name
@REM              This log file is only used if the server is run in the
@REM              background via the start action. 
@REM              The default value is console.log
@REM
@REM WLP_USER_DIR - The user/custom configuration directory used to store
@REM              shared and server-specific configuration. 
@REM              See README.TXT for details about shared resource locations.
@REM              A server's configuration is at %WLP_USER_DIR%\servers\serverName
@REM              The default value is the usr directory in the install directory.
@REM
@REM WLP_OUTPUT_DIR - The directory containing output files for defined servers.
@REM              This directory must have both read and write permissions for
@REM              the user or users that start servers.
@REM              By default, a server's output logs and workarea are stored
@REM              in the %WLP_USER_DIR%\servers\serverName directory
@REM              (alongside configuration and applications).
@REM              If this variable is set, the output logs and workarea 
@REM              would be stored in %WLP_OUTPUT_DIR%\serverName.
@REM
@REM WLP_DEBUG_ADDRESS - The port to use when running the server in debug mode.
@REM              The default value is 7777.
@REM
@REM WLP_DEBUG_SUSPEND - Whether to suspend the jvm on startup or not. This can be
@REM              set to y to suspend the jvm on startup until a debugger attaches,
@REM              or set to n to startup without waiting for a debugger to attach.
@REM              The default value is y.
@REM
@REM WLP_DEBUG_REMOTE - Whether to allow remote debugging or not. This can be set
@REM              to y to allow remote debugging. The default value is n. 
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
if defined WLP_OUTPUT_DIR set WLP_OUTPUT_DIR=!WLP_OUTPUT_DIR:"=!
if defined WLP_DEBUG_ADDRESS set WLP_DEBUG_ADDRESS=!WLP_DEBUG_ADDRESS:"=!

@REM Consume script parameters

@REM Set the action - must be present
set ACTION=%~1
if "%ACTION%" == "" set ACTION=help:usage

@REM Set the server name - optional
set SERVER_ARG=%2
set SERVER_NAME=%~2
if not defined SERVER_ARG (
  set SERVER_NAME=defaultServer
) else if "%SERVER_NAME%" == "" (
  set SERVER_NAME=defaultServer
) else if "%SERVER_NAME:~0,1%" == "-" (
  set SERVER_NAME=defaultServer
)

@REM Set JAVA_PARAMS_QUOTED.
set JAVA_AGENT_QUOTED="-javaagent:!WLP_INSTALL_DIR!\bin\tools\ws-javaagent.jar"
if defined WLP_SKIP_BOOTSTRAP_AGENT set JAVA_AGENT_QUOTED=
set JAVA_PARAMS_QUOTED=!JVM_ARGS! -jar "%WLP_INSTALL_DIR%\bin\tools\ws-server.jar"
set RC=255

@REM process the selected option...
if "help" == "%ACTION%" (
  call:help
) else if "help:usage" == "%ACTION%" (
  call:usage
) else if "version" == "%ACTION%" (
  call:version
) else if "list" == "%ACTION%" (
  call:list
) else if "create" == "%ACTION%" (
  call:createServer
) else if "run" == "%ACTION%" (
  call:runServer
) else if "debug" == "%ACTION%" (
  if not defined WLP_DEBUG_ADDRESS set WLP_DEBUG_ADDRESS=7777
  if not defined WLP_DEBUG_SUSPEND set WLP_DEBUG_SUSPEND=y
  if not defined WLP_DEBUG_REMOTE set WLP_DEBUG_REMOTE_HOST="0.0.0.0:"
  if not defined WLP_DEBUG_REMOTE_HOST set WLP_DEBUG_REMOTE_HOST=""
  set JAVA_PARAMS_QUOTED=-Dwas.debug.mode=true -Dcom.ibm.websphere.ras.inject.at.transform=true -Dsun.reflect.noInflation=true -agentlib:jdwp=transport=dt_socket,server=y,suspend="!WLP_DEBUG_SUSPEND!",address="!WLP_DEBUG_REMOTE_HOST!!WLP_DEBUG_ADDRESS!" !JAVA_PARAMS_QUOTED!
  call:runServer
) else if "status" == "%ACTION%" (
  call:serverStatus
) else if "status:fast" == "%ACTION%" (
  call:serverStatusFast
) else if "start" == "%ACTION%" (
  call:startServer
) else if "package" == "%ACTION%" (
  call:package
) else if "stop" == "%ACTION%" (
  call:stopServer
) else if "dump" == "%ACTION%" (
  call:dump
) else if "javadump" == "%ACTION%" (
  call:javadump
) else if "registerWinService" == "%ACTION%" (
  call:registerWinService
) else if "startWinService" == "%ACTION%" (
  call:startWinService
) else if "stopWinService" == "%ACTION%" (
  call:stopWinService
) else if "unregisterWinService" == "%ACTION%" (
  call:unregisterWinService
) else if "pause" == "%ACTION%" (
  call:pauseServer
) else if "resume" == "%ACTION%" (
  call:resumeServer
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
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --help %SERVER_NAME%
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:version
  call:installEnv
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --version
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:list
  call:installEnv
  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --list
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:serverStatus
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! "%SERVER_NAME%" --status
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:serverStatusFast
  call:serverEnv
  call:serverExists
  if %RC% == 2 goto:eof

  call:serverRunning
goto:eof

:createServer
  call:installEnv

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--create !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:runServer
  call:serverEnvAndJVMOptions
  if not %RC% == 0 goto:eof
  call:serverExists true
  if %RC% == 2 goto:eof

  call:serverWorkingDirectory
  set SAVE_IBM_JAVA_OPTIONS=!IBM_JAVA_OPTIONS!
  set IBM_JAVA_OPTIONS=!SERVER_IBM_JAVA_OPTIONS!
  !JAVA_CMD_QUOTED! !JAVA_AGENT_QUOTED! !JVM_OPTIONS! !JAVA_PARAMS_QUOTED! --batch-file !PARAMS_QUOTED!
  set RC=%errorlevel%
  set IBM_JAVA_OPTIONS=!SAVE_IBM_JAVA_OPTIONS!
  call:javaCmdResult
goto:eof

:startServer
  call:serverEnvAndJVMOptions
  if not %RC% == 0 goto:eof
  call:serverExists true
  if %RC% == 2 goto:eof

  call:serverRunning
  if %RC% == 0 (
    !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --message:info.serverIsAlreadyRunning "%SERVER_NAME%"
    set RC=!errorlevel!
    if !RC! == 0 (
      set RC=1
    ) else (
      call:javaCmdResult
    )
  ) else (
    call:serverWorkingDirectory

    if not exist "%SERVER_OUTPUT_DIR%\workarea" mkdir "%SERVER_OUTPUT_DIR%\workarea"
    type nul > "%SERVER_OUTPUT_DIR%\workarea\.sLock"
    del "%SERVER_OUTPUT_DIR%\workarea\.sCommand" 2> nul

    if not exist "%X_LOG_DIR%" mkdir "%X_LOG_DIR%"

    @REM Ensure we can write to console.log.  If we can't, then the background
    @REM process will fail.  The type command doesn't set errorlevel by itself,
    @REM so use ||.
    (type nul > "%X_LOG_DIR%\%X_LOG_FILE%") 2> nul || rem
    if not !errorlevel! == 0 (
      !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --message:error.fileNotFound "%X_LOG_DIR%\%X_LOG_FILE%"
      set RC=!errorlevel!
      call:javaCmdResult
      goto:eof
    )

    set X_CMD=!JAVA_CMD_QUOTED! !JAVA_AGENT_QUOTED! !JVM_OPTIONS! !JAVA_PARAMS_QUOTED! --batch-file !PARAMS_QUOTED!
    set SAVE_IBM_JAVA_OPTIONS=!IBM_JAVA_OPTIONS!
    set IBM_JAVA_OPTIONS=!SERVER_IBM_JAVA_OPTIONS!

    @REM Use javaw so command windows can be closed.
    start /min /b "" !JAVA_CMD_QUOTED!w !JAVA_AGENT_QUOTED! !JVM_OPTIONS! !JAVA_PARAMS_QUOTED! --batch-file !PARAMS_QUOTED! >> "%X_LOG_DIR%\%X_LOG_FILE%" 2>&1

    set IBM_JAVA_OPTIONS=!SAVE_IBM_JAVA_OPTIONS!

    !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! "!SERVER_NAME!" --status:start
    set RC=!errorlevel!
    call:javaCmdResult
  )
goto:eof

:stopServer
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--stop !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:package
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--package !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:dump
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--dump !PARAMS_QUOTED! 
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:javadump
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--javadump !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof


:registerWinService
  if NOT "%OS%" == "Windows_NT" goto:eof
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof
  "!WLP_INSTALL_DIR!\bin\tools\win\prunsrv.exe"  //IS//%SERVER_NAME% --Startup=manual --DisplayName="%SERVER_NAME%" --Description="Open Liberty" ++DependsOn=Tcpip --LogPath="%WLP_INSTALL_DIR%\usr\servers\%SERVER_NAME%\logs" --StdOutput=auto --StdError=auto --StartMode=exe --StartPath="%WLP_INSTALL_DIR%" --StartImage="%WLP_INSTALL_DIR%\bin\server.bat" ++StartParams=start#%SERVER_NAME% --StopMode=exe --StopPath="%WLP_INSTALL_DIR%" --StopImage="%WLP_INSTALL_DIR%\bin\server.bat" ++StopParams=stop#%SERVER_NAME%                                                                                                                             
  set RC=!errorlevel!
goto:eof

:startWinService
  if NOT "%OS%" == "Windows_NT" goto:eof
  call:serverEnvAndJVMOptions
  if not %RC% == 0 goto:eof
  call:serverExists true
  if %RC% == 2 goto:eof
  call:serverRunning
  if %RC% == 0 (
    !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --message:info.serverIsAlreadyRunning "%SERVER_NAME%"
    set RC=!errorlevel!
    if !RC! == 0 (
      set RC=1
    ) else (
      call:javaCmdResult
    )
  ) else (
     "!WLP_INSTALL_DIR!\bin\tools\win\prunsrv.exe" //ES//%SERVER_NAME%
     set RC=!errorlevel!
     call:serverRunning
     call:javaCmdResult
  )   
goto:eof

:stopWinService
  if NOT "%OS%" == "Windows_NT" goto:eof
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof
  "!WLP_INSTALL_DIR!\bin\tools\win\prunsrv.exe" //SS//%SERVER_NAME%
  set RC=!errorlevel!
goto:eof

:unregisterWinService
  if NOT "%OS%" == "Windows_NT" goto:eof
  "!WLP_INSTALL_DIR!\bin\tools\win\prunsrv.exe" //DS//%SERVER_NAME%
  set RC=!errorlevel!
goto:eof

:pauseServer
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--pause !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof

:resumeServer
  call:serverEnv
  call:serverExists true
  if %RC% == 2 goto:eof

  !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --batch-file=--resume !PARAMS_QUOTED!
  set RC=%errorlevel%
  call:javaCmdResult
goto:eof


@REM
@REM Utility functions
@REM

@REM
@REM Set environment variables for a non-server or nonexistent server command.
@REM
:installEnv
  call:readServerEnv "%WLP_INSTALL_DIR%\etc\server.env"
  call:installEnvDefaults
  call:serverEnvDefaults
goto:eof

@REM
@REM Set variable defaults for a non-server or nonexistent server command.
@REM
:installEnvDefaults
  call:readServerEnv "%WLP_INSTALL_DIR%\java\java.env"
  call:readServerEnv "%WLP_INSTALL_DIR%\etc\default.env"

  if not defined WLP_DEFAULT_USER_DIR set WLP_DEFAULT_USER_DIR=!WLP_INSTALL_DIR!\usr
  if not defined WLP_USER_DIR set WLP_USER_DIR=!WLP_DEFAULT_USER_DIR!

  if not defined WLP_DEFAULT_OUTPUT_DIR set WLP_DEFAULT_OUTPUT_DIR=!WLP_USER_DIR!\servers
  if not defined WLP_OUTPUT_DIR set WLP_OUTPUT_DIR=!WLP_DEFAULT_OUTPUT_DIR!

  set SERVER_CONFIG_DIR=!WLP_USER_DIR!\servers\!SERVER_NAME!
goto:eof

@REM
@REM Set defaults for server variables.
@REM
:serverEnvDefaults
  set SERVER_OUTPUT_DIR=!WLP_OUTPUT_DIR!\!SERVER_NAME!

  if not defined LOG_DIR (
    set X_LOG_DIR=!SERVER_OUTPUT_DIR!\logs
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
        set WLP_SKIP_MAXPERMSIZE=!WLP_DEFAULT_SKIP_MAXPERMSIZE!
      )
    ) else (
      set JAVA_CMD_QUOTED="%JRE_HOME%\bin\java"
    )
  ) else (
    if exist "%JAVA_HOME%\jre\bin\java.exe" set JAVA_HOME=!JAVA_HOME!\jre
    set JAVA_CMD_QUOTED="!JAVA_HOME!\bin\java"
  )

  @REM Command-line parsing of -Xshareclasses does not allow "," in cacheDir.
  if "!WLP_OUTPUT_DIR:,=!" == "!WLP_OUTPUT_DIR!" (
    set SERVER_IBM_JAVA_OPTIONS=-Xshareclasses:name=liberty-%%u,nonfatal,cacheDir="%WLP_OUTPUT_DIR%\.classCache" -XX:ShareClassesEnableBCI -Xscmx80m !IBM_JAVA_OPTIONS!
  ) else (
    set SERVER_IBM_JAVA_OPTIONS=!IBM_JAVA_OPTIONS!
  )

  @REM Add -Xquickstart -Xnoaot for client JVMs only.  AOT is ineffective if
  @REM JVMs have conflicting options, and it's more important that server JVMs
  @REM be able to use AOT.
  set IBM_JAVA_OPTIONS=-Xquickstart -Xnoaot !IBM_JAVA_OPTIONS!
goto:eof

@REM
@REM Set environment variables for an existing server.
@REM
:serverEnv
  call:readServerEnv "%WLP_INSTALL_DIR%\etc\server.env"
  call:installEnvDefaults

  call:readServerEnv "%WLP_USER_DIR%/shared/server.env"
  call:readServerEnv "%SERVER_CONFIG_DIR%\server.env"
  call:serverEnvDefaults
goto:eof

@REM
@REM Set environment variables and JVM_OPTIONS for an existing server.
@REM
:serverEnvAndJVMOptions
  call:serverEnv

  @REM By default, set -XX:MaxPermSize.  This option should be ignored by JVMs
  @REM that don't support the option.
  if not defined WLP_SKIP_MAXPERMSIZE (
    set JVM_OPTIONS=-XX:MaxPermSize=256m
  ) else (
    set JVM_OPTIONS=
  )
  @REM Avoid HeadlessException.
  set JVM_OPTIONS=-Djava.awt.headless=true !JVM_OPTIONS!
  @REM allow late self attach for when the localConnector-1.0 feature is enabled
  set JVM_OPTIONS=-Djdk.attach.allowAttachSelf=true !JVM_OPTIONS!


  @REM The order of merging the jvm.option files sets the precedence. 
  @REM Once a given jvm option is set, it will be overridden if a duplicate
  @REM is seen later. They will both be written in to the options parameter
  @REM but the last one written will take precedence.  If none are read
  @REM the script will try to read etc

  set RC=0
  call:mergeJVMOptions "%WLP_INSTALL_DIR%\usr\shared\jvm.options"
  if not %RC% == 0 goto:eof
  
  call:mergeJVMOptions "%SERVER_CONFIG_DIR%\configDropins\defaults\jvm.options"
  if not %RC% == 0 goto:eof
  
  call:mergeJVMOptions "%SERVER_CONFIG_DIR%\jvm.options"
  if not %RC% == 0 goto:eof
  
  call:mergeJVMOptions "%SERVER_CONFIG_DIR%\configDropins\overrides\jvm.options"
  if not %RC% == 0 goto:eof
  
  @REM If none of the four files above are seen we will check for an options
  @REM file in the etc folder.
  if not defined USE_ETC_OPTIONS (
    set JVM_TEMP_OPTIONS=
    call:mergeJVMOptions "%WLP_INSTALL_DIR%\etc\jvm.options"
  )
  
  @REM If we are running on Java 9, apply Liberty's built-in java 9 options
  if exist "%JAVA_HOME%\lib\modules" (
    call:mergeJVMOptions "%WLP_INSTALL_DIR%\lib\platform\java\java9.options"
  )

  @REM Filter off all of the -D and -X arguments off of !PARAMS_QUOTED! and
  @REM add them onto !JVM_OPTIONS!
  set INCLUDE_NEXT_ARG=F
  for %%a in (%PARAMS_QUOTED%) do (
    set CUR_ARG=%%a
    if "!INCLUDE_NEXT_ARG!"=="T" (
      set JVM_TEMP_OPTIONS=!JVM_TEMP_OPTIONS!=!CUR_ARG!
      set INCLUDE_NEXT_ARG=F
    ) else if "!CUR_ARG:~0,2!"=="-D" (
      @REM key=value arguments get parsed as two separate tokens, so when we see
      @REM a -Dkey=value option we need to set a flag to include the next arg
	  set JVM_TEMP_OPTIONS=!JVM_TEMP_OPTIONS! !CUR_ARG!
	  set INCLUDE_NEXT_ARG=T
    ) else if "!CUR_ARG:~0,2!"=="-X" (
      set JVM_TEMP_OPTIONS=!JVM_TEMP_OPTIONS! !CUR_ARG!
    ) 
  )

  set JVM_OPTIONS=!JVM_OPTIONS!%JVM_TEMP_OPTIONS%
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
@REM Read and set variables from the quoted file %1.  Empty lines and lines
@REM beginning with the hash character ('#') are ignored.  All other lines must
@REM be be of the form: VAR=VALUE
@REM
:readServerEnv
  if not exist %1 goto:eof
  for /f "usebackq eol=# delims== tokens=1,*" %%i in (%1) do set %%i=%%j
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
@REM Set the current working directory for an existing server.
@REM
:serverWorkingDirectory
  if not exist "%SERVER_OUTPUT_DIR%" mkdir "%SERVER_OUTPUT_DIR%"
  cd /d "%SERVER_OUTPUT_DIR%"
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
@REM serverRunning: Return 0 if the server is running (.sLock file is in use), 
@REM                1 if not (file is not in use),
@REM
:serverRunning
  set SERVER_LOCK_FILE=!SERVER_OUTPUT_DIR!\workarea\.sLock
  if NOT EXIST "%SERVER_LOCK_FILE%" (
    set RC=1
  ) else (
    @REM If the server has locked .sLock, then the redirection will fail.  The
    @REM type command doesn't set errorlevel by itself, so use ||.
    (type nul > "%SERVER_LOCK_FILE%") 2> nul || rem
    if !errorlevel! == 0 (
      set RC=1
    ) else (
      set RC=0
    )
  )
goto:eof

@REM
@REM serverExists: Return 0 if %SERVER_CONFIG_DIR% exists, or is "defaultServer" 
@REM                2 if server does not exist
@REM
:serverExists
  if "%SERVER_NAME%" == "defaultServer" (
    set RC=0
  ) else if NOT EXIST "%SERVER_CONFIG_DIR%" (
    if "%1" == "true" (
      !JAVA_CMD_QUOTED! !JAVA_PARAMS_QUOTED! --message:info.serverNotExist "%SERVER_NAME%"
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
