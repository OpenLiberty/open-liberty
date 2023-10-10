-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jdbc-4.1
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0, \
    io.openliberty.jakartaeeClient.internal-7.0, \
    io.openliberty.jakartaee.internal-7.0, \
    com.ibm.websphere.appserver.jdbc-4.1
kind=noship
edition=full
