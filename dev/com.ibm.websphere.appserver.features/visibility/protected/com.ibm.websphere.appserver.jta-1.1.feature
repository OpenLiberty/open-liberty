-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jta-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
singleton=true
IBM-API-Package: javax.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-bundles=com.ibm.websphere.javaee.transaction.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.transaction:jta:1.1"
-jars=com.ibm.websphere.appserver.api.transaction; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.transaction_1.1-javadoc.zip
kind=ga
edition=core
