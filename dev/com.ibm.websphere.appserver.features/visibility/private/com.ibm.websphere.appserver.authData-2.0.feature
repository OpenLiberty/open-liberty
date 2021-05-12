-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.authData-2.0
singleton=true
Subsystem-Version: 2.0.0
-features=\
 com.ibm.websphere.appserver.jcaSecurity-1.0, \
 com.ibm.websphere.appserver.transaction-2.0
-bundles=\
 com.ibm.ws.security.authentication, \
 io.openliberty.security.jaas.internal.common, \
 io.openliberty.security.auth.internal.data
kind=beta
edition=base
