-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-3.0
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-5.0
-bundles=io.openliberty.org.eclipse.microprofile.opentracing.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.opentracing:microprofile-opentracing-api:3.0"
kind=noship
edition=core
WLP-Activation-Type: parallel
