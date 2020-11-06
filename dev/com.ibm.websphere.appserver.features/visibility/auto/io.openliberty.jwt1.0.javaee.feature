-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jwt-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.security.jwt, \
  com.ibm.ws.security.common
-jars=\
  com.ibm.websphere.appserver.api.jwt; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jwt_1.1-javadoc.zip
kind=ga
edition=core
