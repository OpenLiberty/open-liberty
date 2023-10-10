-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jdbc
visibility=public
IBM-ShortName: jdbc
Subsystem-Name: jdbc
-features=io.openliberty.unversioned.jdbc-0.0; ibm.tolerates:="4.1,4.2"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
