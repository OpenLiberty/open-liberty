-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.servlet-3.1
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0, \
    io.openliberty.microProfile.internal-1.4, \
    io.openliberty.microProfile.internal-1.3, \
    io.openliberty.microProfile.internal-1.2, \
    io.openliberty.jakartaee.internal-7.0, \
    com.ibm.websphere.appserver.servlet-3.1
kind=noship
edition=full
