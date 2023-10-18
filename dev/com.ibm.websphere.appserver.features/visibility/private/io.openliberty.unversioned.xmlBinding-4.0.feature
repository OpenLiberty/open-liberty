-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.xmlBinding-4.0
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaeeClient.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.jakartaee.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.xmlBinding-4.0
kind=noship
edition=full
