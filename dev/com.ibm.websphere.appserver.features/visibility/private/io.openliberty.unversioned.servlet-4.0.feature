-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.servlet-4.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-8.0, \
    io.openliberty.microProfile.internal-2.0; ibm.tolerates:="2.1,2.2,3.0,3.2,3.3,4.0,4.1", \
    io.openliberty.jakartaee.internal-8.0, \
    com.ibm.websphere.appserver.servlet-4.0
kind=noship
edition=full
