-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jaxrsClient-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-1.3; ibm.tolerates:="1.4", \
    com.ibm.websphere.appserver.jaxrsClient-2.0
kind=noship
edition=full
