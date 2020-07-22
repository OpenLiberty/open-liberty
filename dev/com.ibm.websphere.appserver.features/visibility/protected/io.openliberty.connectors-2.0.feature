-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors-2.0
visibility=protected
singleton=true
IBM-API-Package: jakarta.resource; type="spec", \
 jakarta.resource.cci; type="spec", \
 jakarta.resource.spi; type="spec", \
 jakarta.resource.spi.endpoint; type="spec", \
 jakarta.resource.spi.security; type="spec", \
 jakarta.resource.spi.work; type="spec"
-features=io.openliberty.jakarta.connectors-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
kind=beta
edition=core
