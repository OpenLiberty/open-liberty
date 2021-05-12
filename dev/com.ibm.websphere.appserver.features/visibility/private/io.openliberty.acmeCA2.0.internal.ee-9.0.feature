-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.acmeCA2.0.internal.ee-9.0
Subsystem-Version: 9.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  io.openliberty.security.acme.internal
kind=beta
edition=base
