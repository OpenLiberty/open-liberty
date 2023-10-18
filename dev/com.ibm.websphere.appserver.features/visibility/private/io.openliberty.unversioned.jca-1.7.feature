-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jca-1.7
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaee.internal-7.0; ibm.tolerates:="8.0,8.0", \
    com.ibm.websphere.appserver.jca-1.7
kind=noship
edition=full
