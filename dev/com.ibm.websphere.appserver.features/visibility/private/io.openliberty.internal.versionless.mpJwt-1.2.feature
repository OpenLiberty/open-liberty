-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpJwt-1.2
visibility=private
singleton=true
-features= \
    io.openliberty.internal.versionlessMP-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.mpJwt-1.2
kind=beta
edition=base
