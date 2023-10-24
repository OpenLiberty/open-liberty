-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.nosql-1.0
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0",\
  io.openliberty.jakarta.annotation-2.1; ibm.tolerates:="3.0",\
  io.openliberty.jakarta.cdi-4.0; ibm.tolerates:="4.1",\
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.jsonp.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.json:jakarta.json-api:2.1.1",\
  io.openliberty.jakarta.nosql.1.0; location:="dev/api/spec/,lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel
