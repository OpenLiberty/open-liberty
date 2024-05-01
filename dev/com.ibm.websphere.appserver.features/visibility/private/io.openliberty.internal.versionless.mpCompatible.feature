-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.versionless.mpCompatible
visibility=private
singleton=true
-features= \
    io.openliberty.mpCompatible-0.0; ibm.tolerates:="1.0,1.2,1.3,1.4,2.0,2.1,2.2,3.0,3.2,3.3,4.0,4.1,5.0,6.0,6.1"
kind=beta
edition=base
