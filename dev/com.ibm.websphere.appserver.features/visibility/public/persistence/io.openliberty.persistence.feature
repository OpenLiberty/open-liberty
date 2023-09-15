-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistence
visibility=public
IBM-ShortName: persistence
Subsystem-Name: Jakarta Persistence
-features=io.openliberty.unversioned.persistence-0.0; ibm.tolerates:="2.0,2.1,2.2,3.0,3.1,3.2", \
  com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2, 4.3"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full