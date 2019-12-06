-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpGraphQL-1.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.graphql;  type="stable"
IBM-ShortName: mpGraphQL-1.0
Subsystem-Name: MicroProfile GraphQL 1.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.graphql-1.0, \
 com.ibm.websphere.appserver.cdi-2.0,\
 com.ibm.websphere.appserver.javax.annotation-1.3, \
 com.ibm.websphere.appserver.jsonb-1.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=com.ibm.ws.require.java8, \
 com.ibm.websphere.javaee.websocket.1.1; apiJar=false; location:="dev/api/spec/,lib/", \
 com.ibm.ws.com.fasterxml.jackson.2.9.1, \
 com.ibm.ws.io.leangen.graphql.spqr.0.9.9, \
 com.ibm.ws.io.leangen.geantyref.1.3, \
 com.ibm.ws.org.reactivestreams.reactive-streams.1.0, \
 com.ibm.ws.io.github.classgraph.classgraph.4.6, \
 com.ibm.ws.com.graphql.java.servlet.6.1, \
 com.ibm.ws.com.graphql.java.11.0, \
 com.ibm.ws.microprofile.graphql.1.0, \
 com.ibm.ws.com.google.guava
kind=beta
edition=core
WLP-Activation-Type: parallel
