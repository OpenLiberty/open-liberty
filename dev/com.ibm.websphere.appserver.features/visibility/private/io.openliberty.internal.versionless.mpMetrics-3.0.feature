-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpMetrics-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-4.0; ibm.tolerates:="4.1", \
    com.ibm.websphere.appserver.mpMetrics-3.0
kind=noship
edition=full
