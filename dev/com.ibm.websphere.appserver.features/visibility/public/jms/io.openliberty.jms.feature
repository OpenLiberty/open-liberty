-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jms
visibility=public
IBM-ShortName: jms
Subsystem-Name: jms
-features=io.openliberty.unversioned.jms-0.0; ibm.tolerates:="2.0"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
