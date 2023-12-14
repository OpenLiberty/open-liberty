#!/bin/bash

export JavaTargetVersion=$1

export TESTAPPDIR=$PWD
export WORKSPACEDIR=$TESTAPPDIR/wspace
export TESTPROJDIR=$TESTAPPDIR/jakartatest
export TESTPROJSRCDIR=$TESTPROJDIR/src
export TESTPROJRESDIR=$TESTPROJDIR/resources

export WLP_HOME=$PWD/../../build.image/wlp
export WLP_SPEC=$WLP_HOME/dev/api/spec

export ANNOTATION_JAR=$(find $WLP_SPEC -name 'io.openliberty.jakarta.annotation.2.1*' -print)
export EJB_JAR=$(find $WLP_SPEC -name 'io.openliberty.jakarta.enterpriseBeans.4.0*' -print)
export PERSISTENCE_JAR=$(find $WLP_SPEC -name 'io.openliberty.jakarta.persistence.3.1*' -print)
export SERVLET_JAR=$(find $WLP_SPEC -name 'io.openliberty.jakarta.servlet.6.0*' -print)
export TRANSACTION_JAR=$(find $WLP_SPEC -name 'io.openliberty.jakarta.transaction.2.0*' -print)

export JUNIT_JAR=$PWD/../../cnf/cache/7.0.0/bnd-cache/biz.aQute.junit/biz.aQute.junit-7.0.0.jar
export COMPONENTTEST_JAR=$PWD/../../com.ibm.ws.componenttest.2.0/build/libs/com.ibm.ws.componenttest.2.0.jar

export LIB_CLASSPATH=$ANNOTATION_JAR:$EJB_JAR:$PERSISTENCE_JAR:$SERVLET_JAR:$TRANSACTION_JAR:$JUNIT_JAR:$COMPONENTTEST_JAR

# Create the temporary workspace
echo "Workspace Dir = $WORKSPACEDIR"
mkdir $WORKSPACEDIR
mkdir -p $WORKSPACEDIR/WEB-INF/classes
mkdir -p $WORKSPACEDIR/WEB-INF/classes/io/openliberty/jpa/test/javasupport/model
mkdir -p $WORKSPACEDIR/WEB-INF/classes/io/openliberty/jpa/test/javasupport/web

echo "Copying files from $TESTPROJRESDIR to $WORKSPACEDIR"
cp -R $TESTPROJRESDIR/* $WORKSPACEDIR

echo "Compiling test binaries with target version $JavaTargetVersion"
cd $TESTPROJSRCDIR
#javac -cp $LIB_CLASSPATH -source $JavaTargetVersion -target $JavaTargetVersion *

export PKG=io/openliberty/jpa/test/javasupport/model
cd $TESTPROJSRCDIR/$PKG
javac -cp $LIB_CLASSPATH -source $JavaTargetVersion -target $JavaTargetVersion *
mv *.class $WORKSPACEDIR/WEB-INF/classes/$PKG

export PKG=io/openliberty/jpa/test/javasupport/web
cd $TESTPROJSRCDIR/$PKG
javac -cp $LIB_CLASSPATH:$TESTPROJSRCDIR -source $JavaTargetVersion -target $JavaTargetVersion *
mv *.class $WORKSPACEDIR/WEB-INF/classes/$PKG

cd $WORKSPACEDIR
jar -cf ../JakartaSupportTest_$JavaTargetVersion.war *

echo "Generated JakartaSupportTest_$JavaTargetVersion.war"

cd $TESTAPPDIR
rm -rf $WORKSPACEDIR
