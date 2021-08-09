#! /bin/sh

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
esac

#Find the necessary resources
ANT_HOME=$0
ANT_HOME=${ANT_HOME%/*}

if [ -z "$JAVACMD" ] ; then 
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then 
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD=$JAVA_HOME/jre/sh/java
    else
      JAVACMD=$JAVA_HOME/bin/java
    fi
  else
    JAVACMD=$(which java)
  fi
fi

if [ -z "$INSTALL_LOG_FILE" ] ; then
    INSTALL_LOG_FILE=$ANT_HOME/install-log.xml
fi
 
if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH=$CLASSPATH
fi

# add in the dependency .jar files
LOCALCLASSPATH="${ANT_HOME}/../webapp/WEB-INF/lib/*":$LOCALCLASSPATH
LOCALCLASSPATH="${ANT_HOME}/../dist/webapp/WEB-INF/lib/*":$LOCALCLASSPATH
LOCALCLASSPATH="${ANT_HOME}/../bin/lib/*":$LOCALCLASSPATH

if [ -n "$JAVA_HOME" ] ; then
  if [ -f "$JAVA_HOME/lib/tools.jar" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/tools.jar
  fi

  if [ -f "$JAVA_HOME/lib/classes.zip" ] ; then
    LOCALCLASSPATH=$LOCALCLASSPATH:$JAVA_HOME/lib/classes.zip
  fi

  # OSX hack to make Ant work with jikes
  if $darwin ; then
    OSXHACK="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes"
    if [ -d ${OSXHACK} ] ; then
      for i in ${OSXHACK}/*.jar
      do
        JIKESPATH=$JIKESPATH:$i
      done
    fi
  fi
fi

# supply JIKESPATH to Ant as jikes.class.path
if [ -n "$JIKESPATH" ] ; then
  if [ -n "$ANT_OPTS" ] ; then
    ANT_OPTS="$ANT_OPTS -Djikes.class.path=$JIKESPATH"
  else
    ANT_OPTS=-Djikes.class.path=$JIKESPATH
  fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  ANT_HOME=`cygpath --path --windows "$ANT_HOME"`
  JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
fi

"$JAVACMD" -classpath "$LOCALCLASSPATH" -Dant.home="${ANT_HOME}" -Dlogback.configurationFile=$INSTALL_LOG_FILE $ANT_OPTS org.apache.tools.ant.Main -e -f "${ANT_HOME}/build.xml" "$@"
