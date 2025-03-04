-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.j2eeManagement-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.management.j2ee; type="ibm-api", \
 javax.management.j2ee; type="spec", \
 javax.management.j2ee.statistics; type="spec", \
 org.omg.stub.javax.management.j2ee; type="spec", \
 com.ibm.websphere.endpoint; type="ibm-api", \
 javax.rmi; type="spec", \
 javax.rmi.CORBA; type="spec", \
 org.omg.stub.java.rmi; type="spec", \
 org.omg.BiDirPolicy; type="spec", \
 org.omg.CONV_FRAME; type="spec", \
 org.omg.CORBA; type="spec", \
 org.omg.CORBA.ContainedPackage; type="spec", \
 org.omg.CORBA.ContainerPackage; type="spec", \
 org.omg.CORBA.InterfaceDefPackage; type="spec", \
 org.omg.CORBA.ORBPackage; type="spec", \
 org.omg.CORBA.PollableSetPackage; type="spec", \
 org.omg.CORBA.TypeCodePackage; type="spec", \
 org.omg.CORBA.ValueDefPackage; type="spec", \
 org.omg.CORBA.portable; type="spec", \
 org.omg.CORBA_2_3; type="spec", \
 org.omg.CORBA_2_3.portable; type="spec", \
 org.omg.CORBA_2_4; type="spec", \
 org.omg.CORBA_2_4.portable; type="spec", \
 org.omg.CSI; type="spec", \
 org.omg.CSIIOP; type="spec", \
 org.omg.CosNaming; type="spec", \
 org.omg.CosNaming.NamingContextExtPackage; type="spec", \
 org.omg.CosNaming.NamingContextPackage; type="spec", \
 org.omg.CosTSInteroperation; type="spec", \
 org.omg.CosTransactions; type="spec", \
 org.omg.Dynamic; type="spec", \
 org.omg.DynamicAny; type="spec", \
 org.omg.DynamicAny.DynAnyFactoryPackage; type="spec", \
 org.omg.DynamicAny.DynAnyPackage; type="spec", \
 org.omg.GIOP; type="spec", \
 org.omg.GSSUP; type="spec", \
 org.omg.IIOP; type="spec", \
 org.omg.IOP; type="spec", \
 org.omg.IOP.CodecFactoryPackage; type="spec", \
 org.omg.IOP.CodecPackage; type="spec", \
 org.omg.MessageRouting; type="spec", \
 org.omg.Messaging; type="spec", \
 org.omg.PortableInterceptor; type="spec", \
 org.omg.PortableInterceptor.ORBInitInfoPackage; type="spec", \
 org.omg.PortableServer; type="spec", \
 org.omg.PortableServer.CurrentPackage; type="spec", \
 org.omg.PortableServer.POAManagerFactoryPackage; type="spec", \
 org.omg.PortableServer.POAManagerPackage; type="spec", \
 org.omg.PortableServer.POAPackage; type="spec", \
 org.omg.PortableServer.ServantLocatorPackage; type="spec", \
 org.omg.PortableServer.portable; type="spec", \
 org.omg.SSLIOP; type="spec", \
 org.omg.Security; type="spec", \
 org.omg.SecurityLevel1; type="spec", \
 org.omg.SecurityLevel2; type="spec", \
 org.omg.SendingContext; type="spec", \
 org.omg.SendingContext.CodeBasePackage; type="spec", \
 org.omg.TimeBase; type="spec"
IBM-ShortName: j2eeManagement-1.1
Subsystem-Version: 1.1
Subsystem-Name: J2EE Management 1.1
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.javax.ejb-3.2, \
  com.ibm.websphere.appserver.iiopcommon-1.0
-bundles=com.ibm.ws.management.j2ee, \
 com.ibm.ws.management.j2ee.mbeans; location:=lib/, \
 com.ibm.websphere.javaee.management.j2ee.1.1; location:=dev/api/spec/; mavenCoordinates="javax.management.j2ee:javax.management.j2ee-api:1.1.1"
-jars=com.ibm.websphere.appserver.api.j2eemanagement; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.j2eemanagement_1.1-javadoc.zip
kind=ga
edition=base
WLP-Platform: javaee-7.0,javaee-8.0,jakartaee-8.0
WLP-InstantOn-Enabled: true; type:=beta
