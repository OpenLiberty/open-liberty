-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jaxrs-2.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Java RESTful Services API 2.0
-features=\
  com.ibm.websphere.appserver.javax.servlet-3.1, \
  com.ibm.websphere.appserver.javax.annotation-1.2; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=\
  com.ibm.websphere.appserver.api.jaxrs20; location:="dev/api/ibm/,lib/", \
  com.ibm.websphere.javaee.activation.1.1; require-java:="9"; location:="dev/api/spec/,lib/"; apiJar=false,\
  com.ibm.websphere.javaee.jaxb.2.2; require-java:="9"; location:="dev/api/spec/,lib/"; apiJar=false, \
  com.ibm.websphere.javaee.jaxrs.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.ws.rs:javax.ws.rs-api:2.0.1"
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jaxrs20_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
