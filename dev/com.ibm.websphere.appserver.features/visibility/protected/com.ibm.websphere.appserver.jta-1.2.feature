-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jta-1.2
visibility=protected
singleton=true
IBM-API-Package: javax.transaction;  type="spec", \
 javax.transaction.xa;  type="spec"
-features=com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0
-bundles=com.ibm.websphere.javaee.transaction.1.2; location:="dev/api/spec/,lib/"
-jars=com.ibm.websphere.appserver.api.transaction; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.transaction_1.1-javadoc.zip
kind=ga
edition=core
