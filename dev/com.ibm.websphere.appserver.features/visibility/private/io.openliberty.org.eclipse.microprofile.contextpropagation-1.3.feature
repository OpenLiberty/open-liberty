-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.contextpropagation-1.3
singleton=true
#-features=io.openliberty.mpCompatible-5.0
# TODO use 1.3 instead of 1.2 once it exists
-bundles=\
 io.openliberty.org.eclipse.microprofile.contextpropagation.1.2; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.2"
kind=noship
edition=full
WLP-Activation-Type: parallel
