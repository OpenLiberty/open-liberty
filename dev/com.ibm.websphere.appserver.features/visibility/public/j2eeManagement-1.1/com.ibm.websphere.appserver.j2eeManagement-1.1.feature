-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.j2eeManagement-1.1
visibility=public
IBM-API-Package: com.ibm.websphere.management.j2ee; type="ibm-api", \
 javax.management.j2ee; type="spec", \
 javax.management.j2ee.statistics; type="spec", \
 org.omg.stub.javax.management.j2ee; type="spec"
IBM-ShortName: j2eeManagement-1.1
Subsystem-Version: 1.1
Subsystem-Name: J2EE Management 1.1
-features=com.ibm.websphere.appserver.javax.ejb-3.2, \
 com.ibm.websphere.appserver.iiopcommon-1.0, \
 com.ibm.websphere.appserver.transaction-1.2
-bundles=com.ibm.ws.management.j2ee, \
 com.ibm.ws.management.j2ee.mbeans; location:=lib/, \
 com.ibm.websphere.javaee.management.j2ee.1.1; location:=dev/api/spec/; mavenCoordinates="javax.management.j2ee:javax.management.j2ee-api:1.1.1"
-jars=com.ibm.websphere.appserver.api.j2eemanagement; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.j2eemanagement_1.1-javadoc.zip
kind=ga
edition=base
WLP-Activation-Type: parallel
