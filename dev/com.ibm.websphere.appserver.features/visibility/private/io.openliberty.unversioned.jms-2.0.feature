-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jms-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaee.internal-7.0; ibm.tolerates:="8.0,8.0", \
    com.ibm.websphere.appserver.jms-2.0
kind=noship
edition=full
