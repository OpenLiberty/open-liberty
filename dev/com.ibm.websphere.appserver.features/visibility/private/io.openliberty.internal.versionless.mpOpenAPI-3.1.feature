-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenAPI-3.1
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-6.0; ibm.tolerates:="6.1", \
    io.openliberty.mpOpenAPI-3.1
kind=noship
edition=full
