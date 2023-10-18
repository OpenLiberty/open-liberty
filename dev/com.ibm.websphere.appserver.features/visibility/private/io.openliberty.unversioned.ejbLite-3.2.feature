-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.ejbLite-3.2
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0; ibm.tolerates:="8.0", \
    com.ibm.websphere.appserver.ejbLite-3.2
kind=noship
edition=full
