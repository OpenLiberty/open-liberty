-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoSecurity-1.0
visibility=private
IBM-API-Package: com.ibm.wsspi.security.token; type="ibm-api"
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.security-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.autoSecurityS4U2-1.0
-jars=com.ibm.websphere.appserver.api.security.spnego; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security.spnego_1.1-javadoc.zip
kind=ga
edition=core
