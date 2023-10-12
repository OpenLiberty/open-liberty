-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.managedBeans-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  javax.annotation; type="spec", \
  javax.annotation.security; type="spec", \
  javax.annotation.sql; type="spec", \
  javax.rmi; type="spec"; require-java:="9", \
  javax.rmi.CORBA; type="spec"; require-java:="9", \
  org.omg.stub.java.rmi; type="spec"; require-java:="9", \
  org.omg.BiDirPolicy; type="spec"; require-java:="9", \
  org.omg.CONV_FRAME; type="spec"; require-java:="9", \
  org.omg.CORBA; type="spec"; require-java:="9", \
  org.omg.CORBA.ContainedPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.ContainerPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.InterfaceDefPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.ORBPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.PollableSetPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.TypeCodePackage; type="spec"; require-java:="9", \
  org.omg.CORBA.ValueDefPackage; type="spec"; require-java:="9", \
  org.omg.CORBA.portable; type="spec"; require-java:="9", \
  org.omg.CORBA_2_3; type="spec"; require-java:="9", \
  org.omg.CORBA_2_3.portable; type="spec"; require-java:="9", \
  org.omg.CORBA_2_4; type="spec"; require-java:="9", \
  org.omg.CORBA_2_4.portable; type="spec"; require-java:="9", \
  org.omg.CSI; type="spec"; require-java:="9", \
  org.omg.CSIIOP; type="spec"; require-java:="9", \
  org.omg.CosNaming; type="spec"; require-java:="9", \
  org.omg.CosNaming.NamingContextExtPackage; type="spec"; require-java:="9", \
  org.omg.CosNaming.NamingContextPackage; type="spec"; require-java:="9", \
  org.omg.CosTSInteroperation; type="spec"; require-java:="9", \
  org.omg.CosTransactions; type="spec"; require-java:="9", \
  org.omg.Dynamic; type="spec"; require-java:="9", \
  org.omg.DynamicAny; type="spec"; require-java:="9", \
  org.omg.DynamicAny.DynAnyFactoryPackage; type="spec"; require-java:="9", \
  org.omg.DynamicAny.DynAnyPackage; type="spec"; require-java:="9", \
  org.omg.GIOP; type="spec"; require-java:="9", \
  org.omg.GSSUP; type="spec"; require-java:="9", \
  org.omg.IIOP; type="spec"; require-java:="9", \
  org.omg.IOP; type="spec"; require-java:="9", \
  org.omg.IOP.CodecFactoryPackage; type="spec"; require-java:="9", \
  org.omg.IOP.CodecPackage; type="spec"; require-java:="9", \
  org.omg.MessageRouting; type="spec"; require-java:="9", \
  org.omg.Messaging; type="spec"; require-java:="9", \
  org.omg.PortableInterceptor; type="spec"; require-java:="9", \
  org.omg.PortableInterceptor.ORBInitInfoPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer; type="spec"; require-java:="9", \
  org.omg.PortableServer.CurrentPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer.POAManagerFactoryPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer.POAManagerPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer.POAPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer.ServantLocatorPackage; type="spec"; require-java:="9", \
  org.omg.PortableServer.portable; type="spec"; require-java:="9", \
  org.omg.SSLIOP; type="spec"; require-java:="9", \
  org.omg.Security; type="spec"; require-java:="9", \
  org.omg.SecurityLevel1; type="spec"; require-java:="9", \
  org.omg.SecurityLevel2; type="spec"; require-java:="9", \
  org.omg.SendingContext; type="spec"; require-java:="9", \
  org.omg.SendingContext.CodeBasePackage; type="spec"; require-java:="9", \
  org.omg.TimeBase; type="spec"; require-java:="9", \
  javax.interceptor; type="spec"
IBM-SPI-Package: \
  com.ibm.wsspi.adaptable.module, \
  com.ibm.ws.adaptable.module.structure, \
  com.ibm.wsspi.adaptable.module.adapters, \
  com.ibm.wsspi.artifact, \
  com.ibm.wsspi.artifact.factory, \
  com.ibm.wsspi.artifact.factory.contributor, \
  com.ibm.wsspi.artifact.overlay, \
  com.ibm.wsspi.artifact.equinox.module, \
  com.ibm.wsspi.anno.classsource, \
  com.ibm.wsspi.anno.info, \
  com.ibm.wsspi.anno.service, \
  com.ibm.wsspi.anno.targets, \
  com.ibm.wsspi.anno.util, \
  com.ibm.ws.anno.classsource.specification
IBM-ShortName: managedBeans-1.0
Subsystem-Name: Java EE Managed Bean 1.0
-features=com.ibm.websphere.appserver.javax.ejb-3.1; apiJar=false; ibm.tolerates:="3.2", \
  com.ibm.websphere.appserver.javax.interceptor-1.1; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.managedBeansCore-1.0
-bundles=com.ibm.ws.managedbeans
-files=dev/api/ibm/schema/ibm-managed-bean-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-managed-bean-bnd_1_1.xsd
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
