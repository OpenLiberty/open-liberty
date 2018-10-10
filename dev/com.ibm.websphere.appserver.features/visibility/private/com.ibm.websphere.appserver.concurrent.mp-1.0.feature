-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.0
visibility=private
singleton=true
-bundles=\
  com.ibm.ws.concurrent.mp.1.0
# com.ibm.websphere.org.eclipse.microprofile.concurrency.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.concurrency:microprofile-concurrency:1.0-SNAPSHOT???"
kind=noship
edition=full
