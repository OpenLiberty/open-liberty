-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=\
  com.ibm.websphere.org.eclipse.microprofile.reactive.streams.operators.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.reactive.streams:microprofile-reactive-streams-operators:1.0-M1"
kind=noship
edition=full
