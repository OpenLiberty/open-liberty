-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.managedBeans-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0; ibm.tolerates:="8.0", \
    io.openliberty.jakartaeeClient.internal-7.0; ibm.tolerates:="8.0", \
    com.ibm.websphere.appserver.managedBeans-1.0
kind=noship
edition=full
