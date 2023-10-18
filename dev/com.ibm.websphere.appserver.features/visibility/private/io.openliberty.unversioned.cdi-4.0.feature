-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.cdi-4.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-10.0, \
    io.openliberty.microProfile.internal-6.0; ibm.tolerates:="6.1", \
    io.openliberty.jakartaeeClient.internal-10.0, \
    io.openliberty.cdi-4.0
kind=noship
edition=full
