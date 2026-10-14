-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-4.0
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-4.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0; ibm.tolerates:="12.0"
-bundles=io.openliberty.persistence.4.0.thirdparty; apiJar=false; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:5.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
