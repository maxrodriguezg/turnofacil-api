#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# -----------------------------------------------------------------------------
# Maven Wrapper Start Up Script
# -----------------------------------------------------------------------------

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true ;;
  Darwin*) darwin=true ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JAVA_HOME" ] && JAVA_HOME=$(cygpath --unix "$JAVA_HOME")
fi

# Attempt to find JAVA_HOME
if [ -z "$JAVA_HOME" ] ; then
  if [ -r /etc/alternatives/java ] ; then
    JAVA_HOME=$(readlink -f /etc/alternatives/java | sed 's:/bin/java::')
  elif [ -r /usr/libexec/java_home ] ; then
    JAVA_HOME=$(/usr/libexec/java_home)
  fi
fi

if [ -z "$JAVA_HOME" ] ; then
  echo "Error: JAVA_HOME is not set and could not be found." >&2
  exit 1
fi

# Resolve the wrapper jar location
WRAPPER_DIR="$(cd "$(dirname "$0")/.mvn/wrapper" && pwd)"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"

if [ ! -f "$WRAPPER_JAR" ] ; then
  echo "Error: Could not find maven-wrapper.jar at $WRAPPER_JAR" >&2
  exit 1
fi

# Execute Maven Wrapper
exec "$JAVA_HOME/bin/java" \
  $MAVEN_OPTS \
  -classpath "$WRAPPER_JAR" \
  -Dmaven.multiModuleProjectDirectory="$(pwd)" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"