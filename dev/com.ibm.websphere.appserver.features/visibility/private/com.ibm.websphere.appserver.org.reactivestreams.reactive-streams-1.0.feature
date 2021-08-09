-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.org.reactivestreams.reactive-streams.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.reactivestreams:reactive-streams:1.0.3"
kind=ga
edition=core
WLP-Activation-Type: parallel
