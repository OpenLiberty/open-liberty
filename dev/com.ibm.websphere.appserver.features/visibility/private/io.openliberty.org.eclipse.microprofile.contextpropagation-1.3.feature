-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.contextpropagation-1.3
singleton=true
-features=io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0"
-bundles=\
 io.openliberty.org.eclipse.microprofile.contextpropagation.1.3; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.3"
kind=ga
edition=core
WLP-Activation-Type: parallel
