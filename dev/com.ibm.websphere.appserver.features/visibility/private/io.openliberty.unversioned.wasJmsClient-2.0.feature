-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.wasJmsClient-2.0
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaeeClient.internal-7.0; ibm.tolerates:="8.0", \
    io.openliberty.jakartaee.internal-7.0; ibm.tolerates:="8.0,8.0", \
    com.ibm.websphere.appserver.wasJmsClient-2.0
kind=noship
edition=full
