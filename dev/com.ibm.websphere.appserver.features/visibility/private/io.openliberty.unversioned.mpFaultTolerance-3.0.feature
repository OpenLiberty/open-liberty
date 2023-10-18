-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpFaultTolerance-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.mpFaultTolerance-3.0
kind=noship
edition=full
