-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jdbc-4.2
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-8.0; ibm.tolerates:="9.1,10.0,11.0", \
    io.openliberty.jakartaeeClient.internal-8.0; ibm.tolerates:="10.0,11.0,9.1", \
    io.openliberty.jakartaee.internal-8.0; ibm.tolerates:="9.1,10.0,11.0", \
    com.ibm.websphere.appserver.jdbc-4.2
kind=noship
edition=full
