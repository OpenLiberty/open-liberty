-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpJwt-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-1.4; ibm.tolerates:="2.0,2.1,2.2,3.0,3.2,3.3", \
    com.ibm.websphere.appserver.mpJwt-1.1
kind=beta
edition=core
