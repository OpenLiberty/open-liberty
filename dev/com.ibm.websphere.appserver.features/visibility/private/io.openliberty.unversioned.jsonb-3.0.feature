-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jsonb-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.microProfile.internal-6.0; ibm.tolerates:="6.1", \
    io.openliberty.jakartaeeClient.internal-10.0; ibm.tolerates:="11.0", \
    io.openliberty.jsonb-3.0
kind=noship
edition=full
