-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.contextpropagation-1.2
singleton=true
-features=io.openliberty.mpCompatible-4.0
-bundles=\
 io.openliberty.org.eclipse.microprofile.contextpropagation.1.2; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
