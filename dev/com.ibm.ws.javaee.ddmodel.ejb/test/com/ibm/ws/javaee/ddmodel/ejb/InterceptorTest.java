/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.ws.javaee.dd.common.QName;
import com.ibm.ws.javaee.dd.common.wsclient.Addressing;
import com.ibm.ws.javaee.dd.common.wsclient.PortComponentRef;
import com.ibm.ws.javaee.dd.common.wsclient.ServiceRef;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.Interceptors;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class InterceptorTest extends EJBJarTestBase {

    String ejbRef0 = "<ejb-ref>" +
                     "<ejb-ref-name>ejbRefName0</ejb-ref-name>" +
                     "<ejb-ref-type>Session</ejb-ref-type>" +
                     "<home>home0</home>" +
                     "<remote>remote0</remote>" +
                     "<ejb-link>ejbLink0</ejb-link>" +
                     "</ejb-ref>";

    @Test
    public void testEjbRef() throws Exception {
        EJBJar ejbJar = getEjbJar();
        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);
        List<EJBRef> ejbRefList = interceptor0.getEJBRefs();
        Assert.assertEquals("ejbRefName0", ejbRefList.get(0).getName());
        Assert.assertEquals(EJBRef.KIND_REMOTE, ejbRefList.get(0).getKindValue());
        Assert.assertEquals(EJBRef.TYPE_SESSION, ejbRefList.get(0).getTypeValue());
        Assert.assertEquals("home0", ejbRefList.get(0).getHome());
        Assert.assertEquals("remote0", ejbRefList.get(0).getInterface());
        Assert.assertEquals("ejbLink0", ejbRefList.get(0).getLink());
    }

    String ejbLocalRef0 = "<ejb-local-ref>" +
                          "<ejb-ref-name>ejbLocalRefName0</ejb-ref-name>" +
                          "<ejb-ref-type>Entity</ejb-ref-type>" +
                          "<local-home>localHome0</local-home>" +
                          "<local>local0</local>" +
                          "<ejb-link>ejbLink0</ejb-link>" +
                          "</ejb-local-ref>";

    @Test
    public void testEjbLocalRef() throws Exception {
        EJBJar ejbJar = getEjbJar();
        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);
        List<EJBRef> ejbLocalRefList = interceptor0.getEJBLocalRefs();
        Assert.assertEquals("ejbLocalRefName0", ejbLocalRefList.get(0).getName());
        Assert.assertEquals(EJBRef.KIND_LOCAL, ejbLocalRefList.get(0).getKindValue());
        Assert.assertEquals(EJBRef.TYPE_ENTITY, ejbLocalRefList.get(0).getTypeValue());
        Assert.assertEquals("localHome0", ejbLocalRefList.get(0).getHome());
        Assert.assertEquals("local0", ejbLocalRefList.get(0).getInterface());
        Assert.assertEquals("ejbLink0", ejbLocalRefList.get(0).getLink());
    }

    String portComponentRef0 = "<port-component-ref>" +
                               "<service-endpoint-interface>serviceEndpointInterface</service-endpoint-interface>" +
                               "<enable-mtom>true</enable-mtom>" +
                               "<mtom-threshold>42</mtom-threshold>" +
                               "<addressing>" +
                               "<enabled>true</enabled>" +
                               "<required>true</required>" +
                               "<responses>ANONYMOUS</responses>" +
                               "</addressing>" +
                               "</port-component-ref>";

    String serviceRef0 = "<service-ref>" +
                         "<service-interface>serviceInteface0</service-interface>" +
                         "<wsdl-file>wsdlFile0</wsdl-file>" +
                         "<jaxrpc-mapping-file>jaxrpcMappingFile0</jaxrpc-mapping-file>" +
                         "<service-qname xmlns:ns1=\"http://test.ibm.com\">ns1:EchoService</service-qname>" +
                         portComponentRef0 +
                         "</service-ref>";

    /**
     * <ol>
     * <li>&lt;service-name-pattern xmlns:ns1="http://test.ibm.com">ns1:EchoService&lt;/service-name-pattern>
     * <ul>
     * <li>getNamespaceURI() returns "http://test.ibm.com"
     * <li>getLocalPart() returns "EchoService"
     * </ul>
     * </li>
     * <li>&lt;service-name-pattern xmlns:ns1="http://test.ibm.com">ns1:EchoService*&lt;/service-name-pattern>
     * <ul>
     * <li>getNamespaceURI() returns "http://test.ibm.com"
     * <li>getLocalPart() returns "EchoService*"
     * </ul>
     * </li>
     * <li>&lt;service-name-pattern>EchoService&lt;/service-name-pattern>
     * <ul>
     * <li>getNamespaceURI() returns null
     * <li>getLocalPart() returns "EchoService"
     * </ul>
     * </li>
     * <li>&lt;service-name-pattern>*&lt;/service-name-pattern>
     * <ul>
     * <li>getNamespaceURI() returns null
     * <li>getLocalPart() returns "*"
     * </ul>
     * </li>
     * </ol>
     * 
     **/
    @Test
    public void testServiceRef() throws Exception {
        EJBJar ejbJar = getEjbJar();
        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);

        List<ServiceRef> servRefList = interceptor0.getServiceRefs();
        ServiceRef servRef0 = servRefList.get(0);
        Assert.assertEquals("serviceInteface0", servRef0.getServiceInterfaceName());
        Assert.assertEquals("wsdlFile0", servRef0.getWsdlFile());
        Assert.assertEquals("jaxrpcMappingFile0", servRef0.getJaxrpcMappingFile());
        QName qName0 = servRef0.getServiceQname();
        Assert.assertEquals("http://test.ibm.com", qName0.getNamespaceURI());
        Assert.assertEquals("EchoService", qName0.getLocalPart());

        List<PortComponentRef> portComRefList = servRef0.getPortComponentRefs();
        PortComponentRef portComRef0 = portComRefList.get(0);
        Assert.assertEquals("serviceEndpointInterface", portComRef0.getServiceEndpointInterfaceName());
        Assert.assertEquals(true, portComRef0.isSetEnableMtom());
        Assert.assertEquals(true, portComRef0.isEnableMtom());
        Assert.assertEquals(true, portComRef0.isSetMtomThreshold());
        Assert.assertEquals(42, portComRef0.getMtomThreshold());

        Addressing addressing0 = portComRef0.getAddressing();
        Assert.assertEquals(true, addressing0.isEnabled());
        Assert.assertEquals(true, addressing0.isSetEnabled());
        Assert.assertEquals(true, addressing0.isRequired());
        Assert.assertEquals(true, addressing0.isSetRequired());
        Assert.assertEquals(Addressing.ADDRESSING_RESPONSES_ANONYMOUS, addressing0.getAddressingResponsesTypeValue());
    }

    @Test
    public void testEmptyLists() throws Exception {
        EJBJar ejbJar = getEjbJar();
        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);
        Assert.assertEquals(true, interceptor0.getResourceRefs().isEmpty());
        Assert.assertEquals(true, interceptor0.getResourceEnvRefs().isEmpty());
        Assert.assertEquals(true, interceptor0.getMessageDestinationRefs().isEmpty());
        Assert.assertEquals(true, interceptor0.getPersistenceContextRefs().isEmpty());
        Assert.assertEquals(true, interceptor0.getPersistenceUnitRefs().isEmpty());
        Assert.assertEquals(true, interceptor0.getDataSources().isEmpty());
        Assert.assertEquals(true, interceptor0.getDescriptions().isEmpty());
    }

    public void testSessionInterceptor() throws Exception {
        EJBJar ejbJar = getEjbJar();
        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);

        Assert.assertEquals(true, interceptor0.getPostActivate().isEmpty());
        Assert.assertEquals(true, interceptor0.getPrePassivate().isEmpty());
        Assert.assertEquals(true, interceptor0.getAroundInvoke().isEmpty());
        Assert.assertEquals(true, interceptor0.getAroundTimeoutMethods().isEmpty());
    }

    String interceptors0 = "<interceptors>" +
                           "<interceptor>" +
                           "<interceptor-class>interceptorClass0</interceptor-class>" +
                           "<post-construct>" +
                           "<lifecycle-callback-class>postConstructClass0</lifecycle-callback-class>" +
                           "<lifecycle-callback-method>postConstructMethod0</lifecycle-callback-method>" +
                           "</post-construct>" +
                           "<pre-destroy>" +
                           "<lifecycle-callback-class>preDestroyClass0</lifecycle-callback-class>" +
                           "<lifecycle-callback-method>preDestroyMethod0</lifecycle-callback-method>" +
                           "</pre-destroy>" +
                           "<env-entry>" +
                           "<env-entry-type>envEntryType0</env-entry-type>" +
                           "<env-entry-value>envEntryValue0</env-entry-value>" +
                           "<lookup-name>lookupName0</lookup-name>" +
                           "<mapped-name>mappedName0</mapped-name>" +
                           "<injection-target>" +
                           "<injection-target-class>injectionTargetClass0</injection-target-class>" +
                           "<injection-target-name>injectionTargetName0</injection-target-name>" +
                           "</injection-target>" +
                           "</env-entry>" +
                           ejbRef0 +
                           ejbLocalRef0 +
                           serviceRef0 +
                           "</interceptor>" +
                           "</interceptors>";

    String interceptors = EJBJarTest.ejbJar30() +
                          interceptors0 +
                          "</ejb-jar>";

    @Test
    public void testInterceptors() throws Exception {
        EJBJar ejbJar = getEJBJar(interceptors);

        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);
        Assert.assertEquals("interceptorClass0", interceptor0.getInterceptorClassName());
        List<LifecycleCallback> postConList = interceptor0.getPostConstruct();
        List<LifecycleCallback> preDestList = interceptor0.getPreDestroy();

        Assert.assertEquals("postConstructClass0", postConList.get(0).getClassName());
        Assert.assertEquals("postConstructMethod0", postConList.get(0).getMethodName());
        Assert.assertEquals("preDestroyClass0", preDestList.get(0).getClassName());
        Assert.assertEquals("preDestroyMethod0", preDestList.get(0).getMethodName());

        List<EnvEntry> envEntryList = interceptor0.getEnvEntries();
        Assert.assertEquals("envEntryType0", envEntryList.get(0).getTypeName());
        Assert.assertEquals("envEntryValue0", envEntryList.get(0).getValue());
        Assert.assertEquals("lookupName0", envEntryList.get(0).getLookupName());
        Assert.assertEquals("mappedName0", envEntryList.get(0).getMappedName());
        List<InjectionTarget> injTargList = envEntryList.get(0).getInjectionTargets();
        Assert.assertEquals("injectionTargetClass0", injTargList.get(0).getInjectionTargetClassName());
        Assert.assertEquals("injectionTargetName0", injTargList.get(0).getInjectionTargetName());
    }

    EJBJar getEjbJar() throws Exception {
        EJBJar ejbJar = getEJBJar(interceptors);
        return ejbJar;
    }

    @Test
    public void testAroundConstruct() throws Exception {
        List<Interceptor> interceptors = getEJBJar(EJBJarTest.ejbJar32() +
                                                   "<interceptors>" +

                                                   "<interceptor>" +
                                                   "<interceptor-class>interceptor0</interceptor-class>" +
                                                   "</interceptor>" +

                                                   "<interceptor>" +
                                                   "<interceptor-class>interceptor1</interceptor-class>" +
                                                   "<around-construct>" +
                                                   "<lifecycle-callback-class>aroundConstructClass10</lifecycle-callback-class>" +
                                                   "<lifecycle-callback-method>aroundConstructMethod10</lifecycle-callback-method>" +
                                                   "</around-construct>" +
                                                   "</interceptor>" +

                                                   "<interceptor>" +
                                                   "<interceptor-class>interceptor2</interceptor-class>" +
                                                   "<around-construct>" +
                                                   "<lifecycle-callback-class>aroundConstructClass20</lifecycle-callback-class>" +
                                                   "<lifecycle-callback-method>aroundConstructMethod20</lifecycle-callback-method>" +
                                                   "</around-construct>" +
                                                   "<around-construct>" +
                                                   "<lifecycle-callback-class>aroundConstructClass21</lifecycle-callback-class>" +
                                                   "<lifecycle-callback-method>aroundConstructMethod21</lifecycle-callback-method>" +
                                                   "</around-construct>" +
                                                   "</interceptor>" +

                                                   "</interceptors>" +
                                                   "</ejb-jar>").getInterceptors().getInterceptorList();

        Interceptor interceptor0 = interceptors.get(0);
        Assert.assertEquals(interceptor0.getAroundConstruct().toString(), 0, interceptor0.getAroundConstruct().size());

        Interceptor interceptor1 = interceptors.get(1);
        List<LifecycleCallback> aroundConstruct1 = interceptor1.getAroundConstruct();
        Assert.assertEquals(aroundConstruct1.toString(), 1, aroundConstruct1.size());
        LifecycleCallback callback10 = aroundConstruct1.get(0);
        Assert.assertEquals("aroundConstructClass10", callback10.getClassName());
        Assert.assertEquals("aroundConstructMethod10", callback10.getMethodName());

        Interceptor interceptor2 = interceptors.get(2);
        List<LifecycleCallback> aroundConstruct2 = interceptor2.getAroundConstruct();
        Assert.assertEquals(aroundConstruct2.toString(), 2, aroundConstruct2.size());
        LifecycleCallback callback20 = aroundConstruct2.get(0);
        Assert.assertEquals("aroundConstructClass20", callback20.getClassName());
        Assert.assertEquals("aroundConstructMethod20", callback20.getMethodName());
        LifecycleCallback callback21 = aroundConstruct2.get(1);
        Assert.assertEquals("aroundConstructClass21", callback21.getClassName());
        Assert.assertEquals("aroundConstructMethod21", callback21.getMethodName());
    }

    @Test(expected = DDParser.ParseException.class)
    public void testAroundConstructEJB31() throws Exception {
        getEJBJar(EJBJarTest.ejbJar31() +
                  "<interceptors>" +
                  "<interceptor>" +
                  "<interceptor-class>interceptor1</interceptor-class>" +
                  "<around-construct>" +
                  "<lifecycle-callback-class>aroundConstructClass10</lifecycle-callback-class>" +
                  "<lifecycle-callback-method>aroundConstructMethod10</lifecycle-callback-method>" +
                  "</around-construct>" +
                  "</interceptor>" +
                  "</interceptors>" +
                  "</ejb-jar>");
    }
}
