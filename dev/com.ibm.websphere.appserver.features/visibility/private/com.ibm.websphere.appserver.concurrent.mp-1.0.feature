-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent.mp-1.0
visibility=private
singleton=true
-bundles=\
  com.ibm.websphere.org.eclipse.microprofile.concurrency.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="com.ibm.ws.org.eclipse.microprofile.concurrency:microprofile-concurrency-api:0.20181204.144440",\
  com.ibm.ws.concurrent.mp.1.0
kind=noship
edition=full
