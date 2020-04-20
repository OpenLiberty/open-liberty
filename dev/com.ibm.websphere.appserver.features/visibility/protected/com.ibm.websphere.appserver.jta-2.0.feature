-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jta-2.0
visibility=protected
singleton=true
IBM-API-Package: jakarta.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-features=com.ibm.websphere.appserver.jakarta.cdi-3.0
-bundles=com.ibm.websphere.jakartaee.transaction.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.transaction:jakarta.transaction-api:2.0.0-RC1"
-jars=com.ibm.websphere.appserver.api.transaction.jakarta; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.transaction_1.1-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
