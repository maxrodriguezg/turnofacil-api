@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM   http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.

@echo off

@REM -----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM
@REM Required ENV vars:
@REM ------------------
@REM   JAVA_HOME - location of a JDK home dir
@REM
@REM Optional ENV vars
@REM -----------------
@REM   M2_HOME - location of a maven2 installation
@REM   MAVEN_OPTS - parameters passed to the Java VM when running Maven
@REM     e.g. to debug Maven itself, use
@REM       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
@REM   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
@REM -----------------------------------------------------------------------------

@REM set title of command window
title Maven Wrapper

@REM Enable delayed expansion
setlocal enabledelayedexpansion

@REM Check if JAVA_HOME is set
if "%JAVA_HOME%"=="" (
  echo Error: JAVA_HOME not found in your environment. >&2
  echo Please set the JAVA_HOME variable in your environment to match the >&2
  echo location of your Java installation. >&2
  goto error
)

@REM Set default M2_HOME if not set
if "%M2_HOME%"=="" (
  set M2_HOME=%~dp0.mvn
)

@REM Set MAVEN_PROJECTBASEDIR
set MAVEN_PROJECTBASEDIR=%~dp0

@REM Setup the classpath
set MAVEN_CLASSPATH=%M2_HOME%\conf;%M2_HOME%\lib\*

@REM Add the wrapper jar to the classpath
if exist "%M2_HOME%\lib\maven-wrapper.jar" (
  set MAVEN_CLASSPATH=%MAVEN_CLASSPATH%;%M2_HOME%\lib\maven-wrapper.jar
)

@REM Execute Maven
"%JAVA_HOME%\bin\java" %MAVEN_OPTS% -classpath "%MAVEN_CLASSPATH%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*

goto end

:error
exit /b 1

:end
endlocal