-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.config-3.1
singleton=true
# io.openliberty.mpCompatible-x.x comes from io.openliberty.org.eclipse.microprofile.config.3.1.ee features
-features=io.openliberty.org.eclipse.microprofile.config.3.1.ee-10.0; ibm.tolerates:="11.0"
-bundles=io.openliberty.org.eclipse.microprofile.config.3.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:3.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
