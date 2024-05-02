-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpFaultTolerance-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.versionlessMP-1.2; ibm.tolerates:="1.3", \
    com.ibm.websphere.appserver.mpFaultTolerance-1.0
kind=beta
edition=base
