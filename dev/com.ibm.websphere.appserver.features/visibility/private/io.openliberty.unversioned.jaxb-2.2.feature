-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jaxb-2.2
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaeeClient.internal-7.0; ibm.tolerates:="8.0", \
    com.ibm.websphere.appserver.jaxb-2.2
kind=noship
edition=full
