-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appClientSupport-2.0
visibility=public
IBM-API-Package: \
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
  org.omg.TimeBase; type="spec", \
  jakarta.annotation; type="spec", \
  jakarta.annotation.security; type="spec", \
  jakarta.annotation.sql; type="spec"
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
IBM-ShortName: appClientSupport-2.0
Subsystem-Name: Jakarta Application Client Support for Server 2.0
-features=io.openliberty.appclient.appClient-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  com.ibm.websphere.appserver.clientContainerRemoteSupport-1.0, \
  com.ibm.websphere.appserver.injection-2.0
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-9.1,jakartaee-10.0,jakartaee-11.0
WLP-InstantOn-Enabled: true; type:=beta
