-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpGraphQL-2.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.graphql; type="stable", \
 io.smallrye.graphql.api; type="third-party", \
 io.smallrye.graphql.client.typesafe.api;  type="third-party"
IBM-ShortName: mpGraphQL-2.0
Subsystem-Name: MicroProfile GraphQL 2.0
-features= \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.cdi-3.0, \
  io.openliberty.jakarta.annotation-2.0, \
  io.openliberty.jsonb-2.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.mpContextPropagation-1.3, \
  io.openliberty.org.eclipse.microprofile.graphql-2.0

-bundles= \
 com.ibm.ws.com.graphql.java.jakarta, \
 com.ibm.ws.io.smallrye.graphql.jakarta, \
 com.ibm.ws.org.jboss.logging
#already provided by kernel...
#com.ibm.ws.org.jboss.jandex, \
kind=noship
edition=full
WLP-Activation-Type: parallel
