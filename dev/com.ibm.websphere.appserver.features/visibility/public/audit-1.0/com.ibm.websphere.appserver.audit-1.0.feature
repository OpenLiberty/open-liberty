-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.audit-1.0
visibility=public
IBM-ShortName: audit-1.0
Subsystem-Name: Audit 1.0
-features=com.ibm.websphere.appserver.auditCollector-1.0, \
com.ibm.websphere.appserver.appSecurity-2.0, \
com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
-bundles=com.ibm.ws.security.audit.file, \
 com.ibm.ws.security.audit.utils, \
 com.ibm.json4j
kind=ga
edition=core
