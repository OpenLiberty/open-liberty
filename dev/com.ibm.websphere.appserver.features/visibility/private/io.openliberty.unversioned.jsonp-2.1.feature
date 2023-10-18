-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jsonp-2.1
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.microProfile.internal-6.0; ibm.tolerates:="6.1", \
    io.openliberty.jakartaeeClient.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.jsonp-2.1
kind=noship
edition=full
