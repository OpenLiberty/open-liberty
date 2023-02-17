-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jta-2.0
visibility=protected
singleton=true
IBM-API-Package: jakarta.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0", \
  io.openliberty.jakarta.cdi-3.0; ibm.tolerates:="4.0"
-bundles=io.openliberty.jakarta.transaction.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.transaction:jakarta.transaction-api:2.0.1"
-jars=io.openliberty.transaction; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.transaction_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
