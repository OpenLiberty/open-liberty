-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0
visibility=private
singleton=true
-bundles=\
  com.ibm.websphere.org.eclipse.microprofile.reactive.streams.operators.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.reactive.streams:microprofile-reactive-streams-operators:1.0-M1", \
  com.ibm.ws.com.lightbend.microprofile.reactive.streams.zerodep; location:=lib/
kind=noship
edition=full
