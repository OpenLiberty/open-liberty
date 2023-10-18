-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jsonp-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0, \
    io.openliberty.microProfile.internal-1.0; ibm.tolerates:="1.2,1.3,1.4", \
    io.openliberty.jakartaeeClient.internal-7.0, \
    com.ibm.websphere.appserver.jsonp-1.0
kind=noship
edition=full
