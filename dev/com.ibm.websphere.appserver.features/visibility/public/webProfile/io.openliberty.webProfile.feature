-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webProfile
visibility=public
IBM-ShortName: webProfile
Subsystem-Name: webProfile
-features=io.openliberty.unversioned.webProfile-0.0; ibm.tolerates:="10.0,11.0,7.0,8.0,9.1"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
