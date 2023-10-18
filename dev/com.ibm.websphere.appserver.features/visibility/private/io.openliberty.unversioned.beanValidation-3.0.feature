-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.beanValidation-3.0
visibility=private
singleton=true
-features= \
    io.openliberty.webProfile.internal-9.1; ibm.tolerates:="10.0", \
    io.openliberty.jakartaeeClient.internal-9.1; ibm.tolerates:="10.0", \
    io.openliberty.beanValidation-3.0
kind=noship
edition=full
