-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.audit-2.0
visibility=public
singleton=true
IBM-ShortName: audit-2.0
Subsystem-Name: Audit 2.0
-features=com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.audit1.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0, 11.0", \
  com.ibm.websphere.appserver.wimcore-1.0, \
  io.openliberty.auditCollector1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=com.ibm.ws.security.audit.utils, \
  com.ibm.ws.security.audit.file
kind=ga
edition=core
WLP-InstantOn-Enabled: true