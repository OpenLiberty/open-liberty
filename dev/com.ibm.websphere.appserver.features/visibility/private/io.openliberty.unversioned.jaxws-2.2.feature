-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jaxws-2.2
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaee.internal-7.0; ibm.tolerates:="8.0,8.0", \
    com.ibm.websphere.appserver.jaxws-2.2
kind=noship
edition=full
