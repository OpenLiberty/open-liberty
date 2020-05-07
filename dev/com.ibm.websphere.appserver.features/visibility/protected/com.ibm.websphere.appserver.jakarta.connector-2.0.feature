-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta.connector-2.0
visibility=protected
singleton=true
IBM-API-Package: jakarta.resource; type="spec", \
 jakarta.resource.cci; type="spec", \
 jakarta.resource.spi; type="spec", \
 jakarta.resource.spi.endpoint; type="spec", \
 jakarta.resource.spi.security; type="spec", \
 jakarta.resource.spi.work; type="spec"
-features=com.ibm.websphere.appserver.jakarta.connector.internal-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.websphere.jakartaee.connector.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.resource:jakarta.resource-api:2.0.0-RC1"
kind=noship
edition=full
