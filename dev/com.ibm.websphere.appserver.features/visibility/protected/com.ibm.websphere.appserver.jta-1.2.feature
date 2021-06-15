-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jta-1.2
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
singleton=true
IBM-API-Package: javax.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-features=com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0,8.0"
-bundles=com.ibm.websphere.javaee.transaction.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.transaction:javax.transaction-api:1.2"
-jars=com.ibm.websphere.appserver.api.transaction; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.transaction_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
