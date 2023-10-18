-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpConfig-1.3
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-1.4; ibm.tolerates:="2.0,2.1,2.2,3.0,3.2", \
    com.ibm.websphere.appserver.mpConfig-1.3
kind=noship
edition=full
