-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaxrsClient
visibility=public
IBM-ShortName: jaxrsClient
Subsystem-Name: jaxrsClient
-features=io.openliberty.unversioned.jaxrsClient-0.0; ibm.tolerates:="2.0,2.1"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
