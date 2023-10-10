-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.mpMetrics-0.0
visibility=private
singleton=true
-features= \
    io.openliberty.microProfile.internal-0.0, \
    com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0,9.0,10.0"
kind=noship
edition=full
