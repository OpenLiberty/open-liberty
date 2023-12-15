-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.config-3.0
singleton=true
# io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0" comes from io.openliberty.org.eclipse.microprofile.config.3.0.ee features
-features=io.openliberty.org.eclipse.microprofile.config.3.0.ee-9.0; ibm.tolerates:="10.0"
-bundles=io.openliberty.org.eclipse.microprofile.config.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:3.0.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
