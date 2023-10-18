-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.messagingClient-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.jakartaeeClient.internal-9.1; ibm.tolerates:="10.0,11.0", \
    io.openliberty.jakartaee.internal-9.1; ibm.tolerates:="10.0,11.0", \
    io.openliberty.messagingClient-3.0
kind=noship
edition=full
