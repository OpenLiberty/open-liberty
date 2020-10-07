-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.interceptor-1.1
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.interceptor.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.geronimo.specs:geronimo-interceptor_1.1_spec:1.0"
kind=ga
edition=core
