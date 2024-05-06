-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpOpenAPI-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-1.3; ibm.tolerates:="1.4,2.0,2.1", \
    com.ibm.websphere.appserver.mpOpenAPI-1.0
kind=noship
edition=full
