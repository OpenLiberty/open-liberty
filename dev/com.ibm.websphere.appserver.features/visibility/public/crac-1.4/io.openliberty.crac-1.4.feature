-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.crac-1.4
visibility=public
singleton=true
IBM-ShortName: crac-1.4
IBM-Process-Types: server
IBM-API-Package: org.crac;type="stable",\
 org.crac.management;type="stable"
Subsystem-Name: org.crac API 1.4
-bundles=io.openliberty.org.crac.1.4; location:="dev/api/stable/,lib/"; mavenCoordinates="org.crac:crac:1.4.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
