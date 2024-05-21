-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpFaultTolerance-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.internal.mpVersion-2.2; ibm.tolerates:="3.0,3.2", \
    com.ibm.websphere.appserver.mpFaultTolerance-2.0
kind=beta
edition=base
