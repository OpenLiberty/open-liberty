-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWS
visibility=public
IBM-ShortName: restfulWS
Subsystem-Name: restfulWS
-features=io.openliberty.unversioned.restfulWS-0.0; ibm.tolerates:="3.0,3.1,4.0"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
