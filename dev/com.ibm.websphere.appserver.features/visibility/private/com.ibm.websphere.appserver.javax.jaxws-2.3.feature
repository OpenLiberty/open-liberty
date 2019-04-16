-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jaxws-2.3
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Java Web Services API 2.3
-features=\
 com.ibm.websphere.appserver.jaxb-2.3
-bundles=com.ibm.websphere.javaee.jaxws.2.3; location:="dev/api/spec/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.3.0"; apiJar=false
kind=noship
edition=full
