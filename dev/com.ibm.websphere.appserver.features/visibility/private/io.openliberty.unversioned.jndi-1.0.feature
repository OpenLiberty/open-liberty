-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.jndi-1.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-7.0; ibm.tolerates:="8.0,9.1,10.0,11.0", \
    com.ibm.websphere.appserver.jndi-1.0
kind=noship
edition=full
