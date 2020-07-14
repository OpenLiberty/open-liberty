-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.jaxws-3.0
singleton=true
-features=io.openliberty.jakarta.jaxb-3.0; apiJar=false
-bundles=\
 io.openliberty.jakarta.jaxws.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.ws:jakarta.xml.ws-api:3.0.0",\
 io.openliberty.jakarta.saaj.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.soap:jakarta.xml.soap-api:2.0.0",\
 io.openliberty.jakarta.jws.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.jws:jakarta.jws-api:3.0.0"
kind=noship
edition=full
WLP-Activation-Type: parallel
