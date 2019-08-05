-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.optional.corba-1.5
visibility=private
IBM-App-ForceRestart: uninstall, install
IBM-Process-Types: client, server
Subsystem-Name: OMG CORBA APIs and RMI-IIOP API
IBM-API-Package: \
  javax.rmi; type="spec"; require-java:="9",\
  javax.rmi.CORBA; type="spec"; require-java:="9",\
  org.omg.stub.java.rmi; type="spec"; require-java:="9",\
  org.omg.BiDirPolicy; type="spec"; require-java:="9",\
  org.omg.CONV_FRAME; type="spec"; require-java:="9",\
  org.omg.CORBA; type="spec"; require-java:="9",\
  org.omg.CORBA.ContainedPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.ContainerPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.InterfaceDefPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.ORBPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.PollableSetPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.TypeCodePackage; type="spec"; require-java:="9",\
  org.omg.CORBA.ValueDefPackage; type="spec"; require-java:="9",\
  org.omg.CORBA.portable; type="spec"; require-java:="9",\
  org.omg.CORBA_2_3; type="spec"; require-java:="9",\
  org.omg.CORBA_2_3.portable; type="spec"; require-java:="9",\
  org.omg.CORBA_2_4; type="spec"; require-java:="9",\
  org.omg.CORBA_2_4.portable; type="spec"; require-java:="9",\
  org.omg.CSI; type="spec"; require-java:="9",\
  org.omg.CSIIOP; type="spec"; require-java:="9",\
  org.omg.CosNaming; type="spec"; require-java:="9",\
  org.omg.CosNaming.NamingContextExtPackage; type="spec"; require-java:="9",\
  org.omg.CosNaming.NamingContextPackage; type="spec"; require-java:="9",\
  org.omg.CosTSInteroperation; type="spec"; require-java:="9",\
  org.omg.CosTransactions; type="spec"; require-java:="9",\
  org.omg.Dynamic; type="spec"; require-java:="9",\
  org.omg.DynamicAny; type="spec"; require-java:="9",\
  org.omg.DynamicAny.DynAnyFactoryPackage; type="spec"; require-java:="9",\
  org.omg.DynamicAny.DynAnyPackage; type="spec"; require-java:="9",\
  org.omg.GIOP; type="spec"; require-java:="9",\
  org.omg.GSSUP; type="spec"; require-java:="9",\
  org.omg.IIOP; type="spec"; require-java:="9",\
  org.omg.IOP; type="spec"; require-java:="9",\
  org.omg.IOP.CodecFactoryPackage; type="spec"; require-java:="9",\
  org.omg.IOP.CodecPackage; type="spec"; require-java:="9",\
  org.omg.MessageRouting; type="spec"; require-java:="9",\
  org.omg.Messaging; type="spec"; require-java:="9",\
  org.omg.PortableInterceptor; type="spec"; require-java:="9",\
  org.omg.PortableInterceptor.ORBInitInfoPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer; type="spec"; require-java:="9",\
  org.omg.PortableServer.CurrentPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer.POAManagerFactoryPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer.POAManagerPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer.POAPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer.ServantLocatorPackage; type="spec"; require-java:="9",\
  org.omg.PortableServer.portable; type="spec"; require-java:="9",\
  org.omg.SSLIOP; type="spec"; require-java:="9",\
  org.omg.Security; type="spec"; require-java:="9",\
  org.omg.SecurityLevel1; type="spec"; require-java:="9",\
  org.omg.SecurityLevel2; type="spec"; require-java:="9",\
  org.omg.SendingContext; type="spec"; require-java:="9",\
  org.omg.SendingContext.CodeBasePackage; type="spec"; require-java:="9",\
  org.omg.TimeBase; type="spec"; require-java:="9"
-bundles=\
  com.ibm.ws.org.apache.yoko.corba.spec.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.rmi.spec.1.5; require-java:="9",\
  com.ibm.ws.org.apache.yoko.osgi.1.5; require-java:="9"
kind=ga
edition=core
WLP-Activation-Type: parallel
