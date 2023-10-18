-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpFaultTolerance-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-2.2; ibm.tolerates:="3.0,3.2", \
    com.ibm.websphere.appserver.mpFaultTolerance-2.0
kind=noship
edition=full
