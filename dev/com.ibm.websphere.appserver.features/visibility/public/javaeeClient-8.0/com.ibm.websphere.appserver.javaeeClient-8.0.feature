-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeeClient-8.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: javaeeClient-8.0
Subsystem-Name: Java EE 8 Application Client
IBM-API-Package: \
  com.ibm.ws.ejb.portable; type="internal", \
  com.ibm.websphere.endpoint; type="ibm-api", \
  com.ibm.websphere.ejbcontainer; type="ibm-api", \
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
  javax.annotation; type="spec", \
  javax.annotation.security; type="spec", \
  javax.annotation.sql; type="spec", \
  javax.management.j2ee; type="spec", \
  javax.management.j2ee.statistics; type="spec", \
  org.omg.stub.javax.management.j2ee; type="spec", \
  javax.interceptor; type="spec", \
  javax.ejb; type="spec", \
  javax.ejb.embeddable; type="spec", \
  javax.ejb.spi; type="spec", \
  javax.jws; type="spec"; require-java:="9", \
  javax.jws.soap; type="spec"; require-java:="9", \
  javax.xml.soap; type="spec"; require-java:="9", \
  javax.xml.ws.handler; type="spec", \
  javax.xml.ws.http; type="spec", \
  javax.xml.ws.spi; type="spec", \
  javax.xml.ws.handler.soap; type="spec", \
  javax.xml.ws.wsaddressing; type="spec", \
  javax.xml.ws.spi.http; type="spec", \
  javax.xml.ws; type="spec", \
  javax.xml.ws.soap; type="spec", \
  javax.wsdl.extensions.http; type="spec", \
  javax.wsdl.extensions.mime; type="spec", \
  javax.wsdl.extensions.schema; type="spec", \
  javax.wsdl.extensions.soap; type="spec", \
  javax.wsdl.extensions.soap12; type="spec", \
  javax.wsdl.extensions; type="spec", \
  javax.wsdl.factory; type="spec", \
  javax.wsdl.xml; type="spec", \
  javax.wsdl; type="spec"
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
-features=com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.jndiClient-1.0, \
  com.ibm.websphere.appserver.beanValidation-2.0, \
  com.ibm.websphere.appserver.jaxwsClient-2.2, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.jsonb-1.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jpa-2.2, \
  com.ibm.websphere.appserver.j2eeManagementClient-1.1, \
  com.ibm.websphere.appserver.jaxb-2.2, \
  com.ibm.websphere.appclient.appClient-1.0, \
  com.ibm.websphere.appserver.wasJmsClient-2.0, \
  com.ibm.websphere.appserver.ejbRemoteClient-1.0, \
  com.ibm.websphere.appserver.javaMail-1.6, \
  com.ibm.websphere.appserver.managedBeans-1.0
-jars=com.ibm.websphere.appserver.api.ejbcontainer; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.ejbcontainer_1.0-javadoc.zip
kind=ga
edition=base
