-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.annotation-1.1
singleton=true
IBM-Process-Types: server, \
 client
-bundles=com.ibm.websphere.javaee.annotation.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.annotation:javax.annotation-api:1.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
