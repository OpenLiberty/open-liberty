-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpFaultTolerance-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.mpFaultTolerance-3.0
kind=beta
edition=base
