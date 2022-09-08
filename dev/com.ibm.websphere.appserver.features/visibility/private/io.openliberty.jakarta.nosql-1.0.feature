-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.nosql-1.0
visibility=private
singleton=true
#TODO com.ibm.websphere.appserver.eeCompatible-11.0
-features=\
  io.openliberty.jakarta.annotation-2.1,\
  io.openliberty.jakarta.cdi-4.0,\
  io.openliberty.jakarta.interceptor-2.1
-bundles=\
  io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.1.0",\
  io.openliberty.jakarta.nosql.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.nosql.communication:communication-core:1.0.0-b4,jakarta.nosql.communication:communication-column:1.0.0-b4,jakarta.nosql.communication:communication-document:1.0.0-b4,jakarta.nosql.communication:communication-key-value:1.0.0-b4,jakarta.nosql.communication:communication-query:1.0.0-b4,jakarta.nosql.mapping:mapping-core:1.0.0-b4,jakarta.nosql.mapping:mapping-column:1.0.0-b4,jakarta.nosql.mapping:mapping-document:1.0.0-b4,jakarta.nosql.mapping:mapping-key-value:1.0.0-b4"
kind=noship
edition=full
WLP-Activation-Type: parallel
