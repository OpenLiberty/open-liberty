-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.persistence-3.0
singleton=true
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.jakarta.persistence.base-3.0
-bundles=com.ibm.websphere.appserver.thirdparty.eclipselink.3.0; apiJar=false; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:2.7.1"
kind=noship
edition=full
WLP-Activation-Type: parallel
