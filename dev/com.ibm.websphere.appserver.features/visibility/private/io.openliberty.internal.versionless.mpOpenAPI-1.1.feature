-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenAPI-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.internal.versionlessMP-2.2; ibm.tolerates:="3.0,3.2,3.3", \
    com.ibm.websphere.appserver.mpOpenAPI-1.1
kind=noship
edition=base
