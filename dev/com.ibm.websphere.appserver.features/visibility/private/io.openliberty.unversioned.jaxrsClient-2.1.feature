-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jaxrsClient-2.1
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-2.0; ibm.tolerates:="2.1,2.2,3.0,3.2,3.3,4.0,4.1", \
    com.ibm.websphere.appserver.jaxrsClient-2.1
kind=noship
edition=full
