-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.ejb-3.2
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaee.internal-7.0; ibm.tolerates:="8.0,8.0", \
    com.ibm.websphere.appserver.ejb-3.2
kind=noship
edition=full
