-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.autoPasswordUtilities1.0.javaee
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.passwordUtilities-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-features=\
 com.ibm.websphere.appserver.javax.connector-1.6; ibm.tolerates:=1.7
kind=ga
edition=base
