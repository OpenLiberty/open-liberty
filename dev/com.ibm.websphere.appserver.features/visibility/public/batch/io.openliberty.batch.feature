-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batch
visibility=public
IBM-ShortName: batch
Subsystem-Name: batch
-features=io.openliberty.unversioned.batch-0.0; ibm.tolerates:="1.0,2.0,2.1"
WLP-Required-Feature: jakartaPlatform, mpPlatform
kind=noship
edition=full
