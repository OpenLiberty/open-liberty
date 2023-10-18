-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jca
visibility=public
IBM-ShortName: jca
Subsystem-Name: jca
-features=io.openliberty.unversioned.jca-0.0; ibm.tolerates:="1.7"
WLP-Required-Feature: jakartaPlatform, mpPlatform
kind=noship
edition=full
