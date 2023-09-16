-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.crac-1.4
visibility=public
singleton=true
IBM-ShortName: crac-1.4
IBM-Process-Types: server
IBM-API-Package: org.crac;type="stable",\
 org.crac.management;type="stable"
Subsystem-Name: org.crac API 1.4
-bundles=io.openliberty.org.crac
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
