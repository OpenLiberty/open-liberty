-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mdb-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mdb-4.0
IBM-API-Package: com.ibm.ws.ejbcontainer.mdb; type="internal", \
  jakarta.ejb; type="spec", \
  jakarta.ejb.embeddable; type="spec", \
  jakarta.ejb.spi; type="spec", \
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
  jakarta.annotation; type="spec", \
  jakarta.annotation.security; type="spec", \
  jakarta.annotation.sql; type="spec", \
  jakarta.interceptor; type="spec"
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
Subsystem-Category: JakartaEE9Application
-features=com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  io.openliberty.mdb4.0.internal.ee-9.0; ibm.tolerates:="10.0", \
  io.openliberty.ejbCore-2.0, \
  io.openliberty.jakarta.interceptor-2.0; ibm.tolerates:="2.1, 2.2", \
  io.openliberty.jakarta.enterpriseBeans-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.mdb.jakarta, \
 io.openliberty.ejbcontainer.v40.internal
Subsystem-Name: Jakarta Enterprise Beans 4.0 Message-Driven Beans
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true; type:=beta
