-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpFaultTolerance-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-1.2; ibm.tolerates:="1.3", \
    com.ibm.websphere.appserver.mpFaultTolerance-1.0
kind=noship
edition=full
