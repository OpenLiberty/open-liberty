-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.connector-1.6
visibility=protected
singleton=true
IBM-API-Package: javax.resource; type="spec", \
 javax.resource.cci; type="spec", \
 javax.resource.spi; type="spec", \
 javax.resource.spi.endpoint; type="spec", \
 javax.resource.spi.security; type="spec", \
 javax.resource.spi.work; type="spec"
-features=com.ibm.websphere.appserver.javax.connector.internal-1.6, \
 com.ibm.websphere.appserver.javaeeCompatible-6.0
-bundles=com.ibm.websphere.javaee.connector.1.6; location:="dev/api/spec/,lib/"
kind=ga
edition=base
