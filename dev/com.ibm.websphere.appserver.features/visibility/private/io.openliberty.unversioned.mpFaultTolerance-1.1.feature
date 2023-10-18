-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpFaultTolerance-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-1.4; ibm.tolerates:="2.0,2.1", \
    com.ibm.websphere.appserver.mpFaultTolerance-1.1
kind=noship
edition=full
