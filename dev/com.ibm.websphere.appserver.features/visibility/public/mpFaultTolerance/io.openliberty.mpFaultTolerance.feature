-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance
visibility=public
IBM-ShortName: mpFaultTolerance
Subsystem-Name: mpFaultTolerance
-features=io.openliberty.unversioned.mpFaultTolerance-0.0; ibm.tolerates:="1.0,1.1,2.0,2.1,3.0,4.0"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform
kind=noship
edition=full
