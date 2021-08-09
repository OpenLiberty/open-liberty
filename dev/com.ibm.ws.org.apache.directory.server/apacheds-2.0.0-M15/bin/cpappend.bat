echo on
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

rem ---------------------------------------------------------------------------
rem Append to CLASSPATH
rem Borrowed from apache-tomcat
rem
rem $Id: cpappend.bat 747975 2009-02-26 00:20:50Z felixk $
rem ---------------------------------------------------------------------------

rem Process the first argument
if ""%1"" == """" goto end
set ADS_CLASSPATH=%ADS_CLASSPATH%;%1
shift

rem Process the remaining arguments
:setArgs
if ""%1"" == """" goto doneSetArgs
set ADS_CLASSPATH=%ADS_CLASSPATH% %1
shift
goto setArgs
:doneSetArgs
:end
