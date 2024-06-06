-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpFaultTolerance-1.1
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-1.4; ibm.tolerates:="2.0,2.1", \
    com.ibm.websphere.appserver.mpFaultTolerance-1.1
<<<<<<< HEAD
kind=noship
edition=full
=======
kind=beta
edition=core
>>>>>>> 6d1ae1ddd5 (features have correct edition)
