-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-3.1
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.org.eclipse.persistence-3.0; location:=lib/
kind=noship
edition=full
WLP-Activation-Type: parallel
