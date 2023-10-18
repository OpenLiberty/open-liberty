-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.restfulWS-3.1
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-10.0, \
    io.openliberty.microProfile.internal-6.0; ibm.tolerates:="6.1", \
    io.openliberty.restfulWS-3.1
kind=noship
edition=full
