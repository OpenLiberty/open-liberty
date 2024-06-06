-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpRestClient-1.3
visibility=private
singleton=true
-features= \
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-3.0; ibm.tolerates:="3.2", \
    com.ibm.websphere.appserver.mpRestClient-1.3
<<<<<<< HEAD
kind=noship
edition=full
=======
kind=beta
edition=core
>>>>>>> 6d1ae1ddd5 (features have correct edition)
