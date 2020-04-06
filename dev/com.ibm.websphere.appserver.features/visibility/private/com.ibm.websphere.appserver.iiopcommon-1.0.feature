-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.iiopcommon-1.0
IBM-API-Package: javax.rmi; type="spec", \
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
IBM-Process-Types: server, \
 client
-features=com.ibm.websphere.appserver.channelfw-1.0
-bundles=com.ibm.ws.org.apache.yoko.corba.spec.1.5, \
 com.ibm.ws.org.apache.yoko.osgi.1.5, \
 com.ibm.ws.org.apache.servicemix.bundles.bcel.5.2, \
 com.ibm.ws.org.apache.yoko.rmi.impl.1.5, \
 com.ibm.ws.org.apache.yoko.core.1.5, \
 com.ibm.ws.org.apache.yoko.util.1.5, \
 com.ibm.ws.transport.iiop, \
 com.ibm.ws.org.apache.yoko.rmi.spec.1.5
kind=ga
edition=base
WLP-Activation-Type: parallel
