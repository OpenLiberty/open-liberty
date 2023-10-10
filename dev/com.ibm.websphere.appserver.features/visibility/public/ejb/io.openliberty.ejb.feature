-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.ejb
visibility=public
IBM-ShortName: ejb
Subsystem-Name: ejb
-features=io.openliberty.unversioned.ejb-0.0; ibm.tolerates:="3.2"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
