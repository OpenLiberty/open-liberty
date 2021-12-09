-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpGraphQL-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.graphql; type="stable", \
 io.smallrye.graphql.api; type="third-party", \
 io.smallrye.graphql.client.typesafe.api;  type="third-party"
IBM-ShortName: mpGraphQL-1.0
Subsystem-Name: MicroProfile GraphQL 1.0
-features=com.ibm.websphere.appserver.mpConfig-1.4; ibm.tolerates:="2.0", \
  io.openliberty.mpCompatible-0.0; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.javax.annotation-1.3, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.graphql-1.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.jsonb-1.0
-bundles= \
 com.ibm.ws.com.graphql.java, \
 com.ibm.ws.io.smallrye.graphql, \
 com.ibm.ws.org.jboss.logging
#already provided by kernel...
#com.ibm.ws.org.jboss.jandex, \
kind=ga
edition=core
WLP-Activation-Type: parallel
