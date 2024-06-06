-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpFaultTolerance-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-1.2; ibm.tolerates:="1.3", \
    com.ibm.websphere.appserver.mpFaultTolerance-1.0
<<<<<<< HEAD
kind=noship
edition=full
=======
kind=beta
edition=core
>>>>>>> 6d1ae1ddd5 (features have correct edition)
