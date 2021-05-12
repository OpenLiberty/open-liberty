-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.org.eclipse.persistence-3.0; location:=lib/
kind=beta
edition=core
WLP-Activation-Type: parallel
