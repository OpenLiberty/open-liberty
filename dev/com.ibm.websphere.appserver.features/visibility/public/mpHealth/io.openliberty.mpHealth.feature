-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpHealth
visibility=public
IBM-ShortName: mpHealth
Subsystem-Name: mpHealth
-features=io.openliberty.unversioned.mpHealth-1.0; ibm.tolerates:="2.0,2.1,2.2,3.0,3.1,4.0"
WLP-Required-Feature: jakartaPlatform, mpPlatform
kind=beta
edition=core
