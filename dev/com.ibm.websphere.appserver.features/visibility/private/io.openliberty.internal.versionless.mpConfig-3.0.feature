-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpConfig-3.0
visibility=private
singleton=true
-features= \
<<<<<<< HEAD
    io.openliberty.noShip-1.0, \
    io.openliberty.internal.mpVersion-5.0; ibm.tolerates:="6.0", \
    io.openliberty.mpConfig-3.0
kind=noship
edition=full
=======
    io.openliberty.internal.mpVersion-5.0; ibm.tolerates:="6.0,6.1", \
    io.openliberty.mpConfig-3.0
kind=beta
edition=core
>>>>>>> 6d1ae1ddd5 (features have correct edition)
