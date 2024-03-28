-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-3.2
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jsonpInternal-2.1
-bundles=io.openliberty.org.eclipse.persistence-3.2; location:=lib/
kind=beta
edition=core
WLP-Activation-Type: parallel
