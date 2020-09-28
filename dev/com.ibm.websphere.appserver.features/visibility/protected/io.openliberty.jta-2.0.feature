-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jta-2.0
visibility=protected
singleton=true
IBM-API-Package: jakarta.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-features=io.openliberty.jakarta.cdi-3.0
-bundles=io.openliberty.jakarta.transaction.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.transaction:jakarta.transaction-api:2.0.0-RC3"
-jars=com.ibm.websphere.appserver.api.transaction.2.0.jakarta; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.transaction_1.1-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
