-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpMetrics
visibility=public
IBM-ShortName: mpMetrics
Subsystem-Name: mpMetrics
-features=io.openliberty.unversioned.mpMetrics-0.0; ibm.tolerates:="1.0,1.1,2.0,2.2,2.3,3.0,4.0,5.0,5.1"
WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform, eeCompatible
kind=noship
edition=full
