-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi
visibility=public
IBM-ShortName: cdi
Subsystem-Name: cdi
-features=io.openliberty.unversioned.cdi-0.0; ibm.tolerates:="1.2,2.0,3.0,4.0,4.1"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
