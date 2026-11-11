-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.persistence.base-4.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.eeCompatible-11.0; ibm.tolerates:="12.0"
-bundles=io.openliberty.org.hibernate.4.0; location:=lib/
kind=beta
edition=core
WLP-Activation-Type: parallel
