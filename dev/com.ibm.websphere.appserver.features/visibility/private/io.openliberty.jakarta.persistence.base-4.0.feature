-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-4.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jsonpInternal-2.2
-bundles=io.openliberty.org.eclipse.persistence-4.0; location:=lib/
kind=noship
edition=full
WLP-Activation-Type: parallel
