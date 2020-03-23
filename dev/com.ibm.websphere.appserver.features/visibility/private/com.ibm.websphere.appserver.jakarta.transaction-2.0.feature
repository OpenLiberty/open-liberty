-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.transaction-2.0
visibility=private
singleton=true
IBM-API-Package:\
 jakarta.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-bundles=com.ibm.websphere.jakarta.transaction.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.transaction:jakarta.transaction-api:2.0.0-RC1"
kind=ga
edition=core
WLP-Activation-Type: parallel