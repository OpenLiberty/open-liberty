-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.telemetry-1.0
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-5.0
-bundles=io.openliberty.org.eclipse.microprofile.telemetry.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.telemetry:microprofile-telemetry-api:1.0"
kind=noship
edition=full
WLP-Activation-Type: parallel 