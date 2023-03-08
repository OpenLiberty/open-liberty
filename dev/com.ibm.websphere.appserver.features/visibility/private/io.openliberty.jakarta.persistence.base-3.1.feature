-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-3.1
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jsonpInternal-2.1
-bundles=io.openliberty.org.eclipse.persistence-3.1; location:=lib/
kind=ga
edition=core
WLP-Activation-Type: parallel
