-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpFaultTolerance-4.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-5.0; ibm.tolerates:="6.0,6.1", \
    io.openliberty.mpFaultTolerance-4.0
kind=noship
edition=full
