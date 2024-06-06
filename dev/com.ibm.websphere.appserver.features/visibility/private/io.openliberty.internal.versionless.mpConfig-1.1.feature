-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpConfig-1.1
visibility=private
singleton=true
-features= \
<<<<<<< HEAD
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-1.2, \
    com.ibm.websphere.appserver.mpConfig-1.1
kind=noship
edition=full
=======
    io.openliberty.internal.mpVersion-1.2; ibm.tolerates:="1.3,2.2,3.0,3.2", \
    com.ibm.websphere.appserver.mpConfig-1.1
kind=beta
edition=core
>>>>>>> 6d1ae1ddd5 (features have correct edition)
