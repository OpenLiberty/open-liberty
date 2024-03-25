-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaeeClient-11.0
visibility=public
singleton=true
IBM-API-Package: \
  com.ibm.websphere.endpoint; type="ibm-api", \
  com.ibm.websphere.ejbcontainer; type="ibm-api", \
  jakarta.ejb; type="spec", \
  jakarta.ejb.embeddable; type="spec", \
  jakarta.ejb.spi; type="spec", \
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
  jakarta.jws; type="spec", \
  jakarta.jws.soap; type="spec", \
  jakarta.xml.soap; type="spec", \
  jakarta.xml.ws; type="spec", \
  jakarta.xml.ws.handler; type="spec", \
  jakarta.xml.ws.handler.soap; type="spec", \
  jakarta.xml.ws.http; type="spec", \
  jakarta.xml.ws.soap; type="spec", \
  jakarta.xml.ws.spi; type="spec", \
  jakarta.xml.ws.spi.http; type="spec", \
  jakarta.xml.ws.wsaddressing; type="spec", \
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
IBM-ShortName: jakartaeeClient-11.0
Subsystem-Name: Jakarta EE 11.0 Application Client
-features=io.openliberty.cdi-4.1, \
  io.openliberty.enterpriseBeansRemoteClient-2.0, \
  io.openliberty.mail-2.1, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.jakarta.jndiClient-2.0, \
  io.openliberty.jsonb-3.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-3.2, \
  io.openliberty.beanValidation-3.1, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.appclient.appClient-2.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jsonp-2.1
-jars=io.openliberty.ejbcontainer; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.ejbcontainer_1.0-javadoc.zip
kind=noship
edition=full
