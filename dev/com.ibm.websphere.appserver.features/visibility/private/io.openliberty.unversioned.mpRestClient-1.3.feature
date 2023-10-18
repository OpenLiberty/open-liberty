-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpRestClient-1.3
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-3.0; ibm.tolerates:="3.2", \
    com.ibm.websphere.appserver.mpRestClient-1.3
kind=noship
edition=full
