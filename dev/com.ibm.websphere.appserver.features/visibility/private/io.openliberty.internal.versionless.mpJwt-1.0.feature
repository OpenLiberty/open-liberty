-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpJwt-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-1.2; ibm.tolerates:="1.3", \
    com.ibm.websphere.appserver.mpJwt-1.0
 kind=ga
edition=core
