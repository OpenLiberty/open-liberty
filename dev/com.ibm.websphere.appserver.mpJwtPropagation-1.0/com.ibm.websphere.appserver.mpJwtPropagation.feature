-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpJwtPropagation-1.0
visibility=private
-bundles=com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/", \
 com.ibm.ws.security.mp.jwt.propagation, \
 com.ibm.ws.jaxrs.2.0.client
kind=noship
edition=full
