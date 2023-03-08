-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-3.1
singleton=true
IBM-Process-Types: server, \
 client
-features=io.openliberty.jakarta.persistence.base-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=io.openliberty.persistence.3.1.thirdparty; apiJar=false; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:4.0.0"
kind=ga
edition=base
WLP-Activation-Type: parallel
