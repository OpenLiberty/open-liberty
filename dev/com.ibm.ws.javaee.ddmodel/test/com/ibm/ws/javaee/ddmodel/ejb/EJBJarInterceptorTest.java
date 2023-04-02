/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejb;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
import com.ibm.ws.javaee.ddmodel.DDJakarta10Elements;

@RunWith(Parameterized.class)
public class EJBJarInterceptorTest extends EJBJarTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarInterceptorTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    //
    
    private EJBJar interceptorsEJBJar;

    protected EJBJar getInterceptorsEjbJar() throws Exception {
        if ( interceptorsEJBJar == null ) {
            interceptorsEJBJar = parseEJBJarMax( ejbJar30("", interceptorsXML) );
        }
        return interceptorsEJBJar;
    }
    
    //

    protected static final String ejbRefXML =
        "<ejb-ref>" +
            "<ejb-ref-name>ejbRefName0</ejb-ref-name>" +
            "<ejb-ref-type>Session</ejb-ref-type>" +
            "<home>home0</home>" +
            "<remote>remote0</remote>" +
            "<ejb-link>ejbLink0</ejb-link>" +
        "</ejb-ref>";
    
    protected static final String ejbLocalRefXML =
        "<ejb-local-ref>" +
            "<ejb-ref-name>ejbLocalRefName0</ejb-ref-name>" +
            "<ejb-ref-type>Entity</ejb-ref-type>" +
            "<local-home>localHome0</local-home>" +
            "<local>local0</local>" +
            "<ejb-link>ejbLink0</ejb-link>" +
        "</ejb-local-ref>";

    protected static final String portComponentRefXML =
        "<port-component-ref>" +
            "<service-endpoint-interface>serviceEndpointInterface</service-endpoint-interface>" +
            "<enable-mtom>true</enable-mtom>" +
            "<mtom-threshold>42</mtom-threshold>" +
            "<addressing>" +
                "<enabled>true</enabled>" +
                "<required>true</required>" +
                "<responses>ANONYMOUS</responses>" +
            "</addressing>" +
        "</port-component-ref>";

    protected static final String serviceRefXML =
        "<service-ref>" +
            "<service-interface>serviceInteface0</service-interface>" +
            "<wsdl-file>wsdlFile0</wsdl-file>" +
            "<jaxrpc-mapping-file>jaxrpcMappingFile0</jaxrpc-mapping-file>" +
            "<service-qname xmlns:ns1=\"http://test.ibm.com\">ns1:EchoService</service-qname>" +
            portComponentRefXML +
        "</service-ref>";
    
    protected static final String interceptorsXML =
        "<interceptors>" +
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
            ejbRefXML +
            ejbLocalRefXML +
            serviceRefXML +
            "</interceptor>" +
        "</interceptors>";

    protected static final String interceptorsAroundConstructXML =
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
       "</interceptors>";
    
    protected static final String interceptorsAroundConstruct31XML =    
        "<interceptors>" +
            "<interceptor>" +
                "<interceptor-class>interceptor1</interceptor-class>" +
                "<around-construct>" +
                    "<lifecycle-callback-class>aroundConstructClass10</lifecycle-callback-class>" +
                    "<lifecycle-callback-method>aroundConstructMethod10</lifecycle-callback-method>" +
                "</around-construct>" +
            "</interceptor>" +
        "</interceptors>";
    
    //
    
    protected static final String interceptorsEE10XML =
            "<interceptors>" +
                "<interceptor>" +
                    "<interceptor-class>interceptor0</interceptor-class>" +
                    DDJakarta10Elements.CONTEXT_SERVICE_XML +
                    DDJakarta10Elements.MANAGED_EXECUTOR_XML +
                    DDJakarta10Elements.MANAGED_SCHEDULED_EXECUTOR_XML +
                    DDJakarta10Elements.MANAGED_THREAD_FACTORY_XML +                
                "</interceptor>" +
            "</interceptors>";

    //

    @Test
    public void testEjbRef() throws Exception {
        EJBJar ejbJar = getInterceptorsEjbJar();
        
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

    @Test
    public void testEjbLocalRef() throws Exception {
        EJBJar ejbJar = getInterceptorsEjbJar();

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
     **/
    @Test
    public void testServiceRef() throws Exception {
        EJBJar ejbJar = getInterceptorsEjbJar();

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
        EJBJar ejbJar = getInterceptorsEjbJar();

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
        EJBJar ejbJar = getInterceptorsEjbJar();

        Interceptors interceptors = ejbJar.getInterceptors();
        List<Interceptor> intList = interceptors.getInterceptorList();
        Interceptor interceptor0 = intList.get(0);

        Assert.assertEquals(true, interceptor0.getPostActivate().isEmpty());
        Assert.assertEquals(true, interceptor0.getPrePassivate().isEmpty());
        Assert.assertEquals(true, interceptor0.getAroundInvoke().isEmpty());
        Assert.assertEquals(true, interceptor0.getAroundTimeoutMethods().isEmpty());
    }

    @Test
    public void testInterceptors() throws Exception {
        EJBJar ejbJar = getInterceptorsEjbJar();

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

    // 'around-construct' should supported using a 3.2 schema.

    @Test
    public void testAroundConstruct() throws Exception {
        List<Interceptor> interceptors = parseEJBJarMax( ejbJar32("", interceptorsAroundConstructXML) )
            .getInterceptors().getInterceptorList();

        Interceptor interceptor0 = interceptors.get(0);
        Assert.assertEquals(
                interceptor0.getAroundConstruct().toString(),
                0, interceptor0.getAroundConstruct().size() );

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

    // 'around-construct' requires the 3.2 schema.

    @Test
    public void testAroundConstructEJB31At31() throws Exception {
        parseEJBJar( ejbJar31("", interceptorsAroundConstruct31XML),
                     EJBJar.VERSION_3_1,
                     "unexpected.child.element", "CWWKC2259E"); 
    }

    // 'around-construct' support is keyed to the schema version,
    // not the provisioned version.
    
    @Test
    public void testAroundConstructEJB31At32() throws Exception {
        parseEJBJar( ejbJar31("", interceptorsAroundConstruct31XML),
                     EJBJar.VERSION_3_2,
                     "unexpected.child.element", "CWWKC2259E"); 
    }
    
    //

    @Test
    public void testEE10InterceptorsEJB32() throws Exception {
        parseEJBJar( ejbJar32(interceptorsEE10XML), EJBJar.VERSION_3_2,
                "unexpected.child.element",
                "CWWKC2259E", "context-service", "ejb-jar.xml" );
    }
            
    @Test
    public void testEE10InterceptorsEJB40() throws Exception {
        parseEJBJar( ejbJar40(interceptorsEE10XML), EJBJar.VERSION_4_0,
                "unexpected.child.element",
                "CWWKC2259E", "context-service", "ejb-jar.xml" );
    }

// Issue 20386: There is no EJB 5.0 for Jakarta EE 10.
//   
//    @Test
//    public void testEE10InterceptorsEJB50() throws Exception {
//        EJBJar ejbJar = parseEJBJar( ejbJar40(interceptorsEE10XML), EJBJar.VERSION_5_0);
//
//        List<String> names = DDJakarta10Elements.names("EJBJar", "interceptors");
//
//        List<Interceptor> interceptors = ejbJar.getInterceptors().getInterceptorList();
//        DDJakarta10Elements.verifySize(names, 1, interceptors);
//
//        Interceptor interceptor = interceptors.get(0);
//        DDJakarta10Elements.withName(names, "[0]",
//                (useNames) -> DDJakarta10Elements.verifyEE10(useNames, interceptor) );
//    }
}
