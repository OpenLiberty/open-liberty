-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpRestClient-1.3
visibility=private
singleton=true
-features= \
    io.openliberty.internal.versionlessMP-3.0; ibm.tolerates:="3.2", \
    com.ibm.websphere.appserver.mpRestClient-1.3
kind=noship
edition=base
