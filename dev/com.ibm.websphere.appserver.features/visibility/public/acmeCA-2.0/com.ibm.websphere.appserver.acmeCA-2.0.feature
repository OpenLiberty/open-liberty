-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.acmeCA-2.0
visibility=private
singleton=true
IBM-ShortName: acmeCA-2.0
Subsystem-Version: 2.0
Subsystem-Name: Automatic Certificate Management Environment (ACME) Support 2.0
-features=\
  com.ibm.wsspi.appserver.webBundle-1.0,\
  com.ibm.websphere.appserver.appSecurity-1.0
-bundles=\
  com.ibm.ws.security.acme; start-phase:=APPLICATION_EARLY
kind=noship
edition=full