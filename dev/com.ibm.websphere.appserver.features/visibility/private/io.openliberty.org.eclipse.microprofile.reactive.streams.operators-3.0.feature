-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.reactive.streams.operators-3.0
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1", \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0
-bundles=\
  io.openliberty.org.eclipse.microprofile.reactive.streams.operators.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.reactive-streams-operators:microprofile-reactive-streams-operators-api:3.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
