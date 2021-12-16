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
package com.ibm.ws.javaee.ddmodel.managedbean;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup;
import com.ibm.ws.javaee.dd.managedbean.ManagedBean;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.ws.javaee.ddmodel.DDParser;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

public class ManagedBeanBndTest extends DDTestBase {

    /**  */
    private static final String refsXML = "<ejb-ref name=\"AnnotationInjectionInterceptor/ejbLocalRef\" binding-name=\"ejblocal:session/MixedSFInterceptorBean/MixedSFLocal\"/> \n"
                                          +
                                          "<ejb-ref name=\"AnnotationInjectionInterceptor/ejbRemoteRef\" binding-name=\"session/MixedSFInterceptorBean/MixedSFRemote\"/> \n" +
                                          "<resource-ref name=\"AnnotationInjectionInterceptor/jms/WSTestQCF\" binding-name=\"Jetstream/jms/WSTestQCF\"/> \n" +
                                          "<message-destination-ref name=\"AnnotationInjectionInterceptor/jms/RequestQueue\" binding-name=\"Jetstream/jms/RequestQueue\"/> \n" +
                                          "<resource-env-ref name=\"AnnotationInjectionInterceptor/jms/ResponseQueue\" binding-name=\"Jetstream/jms/ResponseQueue\"/> \n";

    private final static String managedBeanXML1 = "<managed-bean id=\"managedBeanID\" class=\"com.ibm.ManagedBean\"> \n" +
                                                  "</managed-bean> \n";

    private final static String managedBeanXML2 =
                    "<managed-bean id=\"managedBeanID\" class=\"com.ibm.ManagedBean\"> \n" + refsXML + "</managed-bean> \n";

    private final static String interceptorXML1 = "<interceptor class=\"suite.r70.base.injection.mix.ejbint.XMLInjectionInterceptor3\"> \n" +
                                                  "</interceptor> \n";

    private final static String interceptorXML2 =
                    "<interceptor class=\"suite.r70.base.injection.mix.ejbint.XMLInjectionInterceptor3\"> \n" + refsXML + "</interceptor> \n";

    private final static String managedBeanBndXML =
                    "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\" xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd \">";

    private final static String managedBeanBndXMLVersion11 =
                    "<managed-bean-bnd id=\"idvalue0\" version=\"1.1\" xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_1.xsd \">";

    private final static String managedBeanBndInvalidXMLNoVersion = "<managed-bean-bnd id=\"idvalue0\" xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd \">";

    private final static String managedBeanBndInvalidXMLWrongRoot = "<managed-bean-bnd-wrong id=\"idvalue0\" xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd \">";

    private final static String managedBeanInvalidNoClass = "<managed-bean id=\"managedBeanID\"> \n" +
                                                            "</managed-bean> \n";
    protected boolean isWarModule = false;

    /*
     * Test the required attribute missing - version attribute
     */
    @Test
    public void testRequiredAttributeVersionMissing() throws Exception {
        try {
            parse(managedBeanBndInvalidXMLNoVersion + "</managed-bean-bnd>");
            fail("The exception of UnableToAdaptException should be thrown.");
        } catch (DDParser.ParseException e) {
            assertTrue("The message key CWWKC2265E should be displayed", e.getMessage().contains("CWWKC2265E"));
        }
    }

    /*
     * Test the required attribute missing - class attribute in managed-bean
     */
    @Test
    public void testRequiredAttributeClassMissing() throws Exception {
        try {
            getManagedBean(managedBeanInvalidNoClass);
            parse(managedBeanBndInvalidXMLNoVersion + "</managed-bean-bnd>");
            fail("The exception of UnableToAdaptException should be thrown.");
        } catch (DDParser.ParseException e) {
            assertTrue("The message key CWWKC2251E should be displayed", e.getMessage().contains("CWWKC2251E"));
        }

    }

    /*
     * Test the wrong root element
     */
    @Test
    public void testWrongRootElement() throws Exception {
        try {
            parse(managedBeanBndInvalidXMLWrongRoot + "</managed-bean-bnd-wrong>");
            fail("The exception of UnableToAdaptException should be thrown.");
        } catch (DDParser.ParseException e) {
            assertTrue("The message key CWWKC2252E should be displayed", e.getMessage().contains("CWWKC2252E"));

        }
    }

    /**
     * Tests that we parse without exception a bindings file of version 1.1 and return that version correctly.
     * 
     * @throws Exception
     */
    @Test
    public void testEmptyBeansList() throws Exception {
        ManagedBeanBnd managedBeanBnd = parse(managedBeanBndXML + "</managed-bean-bnd>");
        Assert.assertNotNull("managed bean list should not be null.", managedBeanBnd.getManagedBeans());
        Assert.assertEquals("managed bean list should be empty.", 0, managedBeanBnd.getManagedBeans().size());
    }

    @Test
    public void testManagedBeanWithNoData() throws Exception {
        // create a snippet of managed bean xml and check it parsed ok

        ManagedBean managedBean = getManagedBean(managedBeanXML1);

        validateEmptyRefBindings(managedBean);
    }

    /**
     * Test the version attributes on the root element of managed bean bnd
     * 
     * @throws Exception
     */
    @Test
    public void testManagedBeanWithData() throws Exception {

        ManagedBean managedBean = getManagedBean(managedBeanXML2);

        validateRefBindings(managedBean);
    }

    @Test
    public void testManagedBeanVersion11() throws Exception {
        ManagedBeanBnd managedBeanBnd = parse(managedBeanBndXMLVersion11 + "</managed-bean-bnd>");
        Assert.assertEquals("Managed beans binding should be of version 1.1.", "1.1", managedBeanBnd.getVersion());
    }

    /**
     * Tests that we return an empty list of interceptors if there are none in the managed bean bnd xml
     * 
     * @throws Exception
     */
    @Test
    public void testEmptyInterceptorsList() throws Exception {
        ManagedBeanBnd managedBeanBnd = parse(managedBeanBndXML + "</managed-bean-bnd>");
        Assert.assertNotNull("Interceptor list should not be null.", managedBeanBnd.getInterceptors());
        Assert.assertEquals("Interceptor list should be empty.", 0, managedBeanBnd.getInterceptors().size());
    }

    @Test
    public void testInterceptorWithNoData() throws Exception {
        Interceptor interceptor = getInterceptor(interceptorXML1);
        validateEmptyRefBindings(interceptor);
    }

    @Test
    public void testInterceptorWithData() throws Exception {
        Interceptor interceptor = getInterceptor(interceptorXML2);

        validateRefBindings(interceptor);
    }

    private ManagedBeanBnd parse(final String xml) throws Exception {
        String path = isWarModule ? ManagedBeanBndAdapter.XML_BND_IN_WEB_MOD_NAME : ManagedBeanBndAdapter.XML_BND_IN_EJB_MOD_NAME;
        final WebModuleInfo moduleInfo = isWarModule ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null;
        return parse(xml, new ManagedBeanBndAdapter(), path, null, null, WebModuleInfo.class, moduleInfo);
    }

    private ManagedBeanBnd getManagedBeanBnd(String jarString) throws Exception {
        return parse(jarString);
    }

    /**
     * @param xmlSnippet
     * @return
     * @throws Exception
     */
    private ManagedBean getManagedBean(String xmlSnippet) throws Exception {
        String xmlString = managedBeanBndXML +
                           xmlSnippet +
                           "</managed-bean-bnd>";
        ManagedBeanBnd managedBeanBnd = getManagedBeanBnd(xmlString);
        List<ManagedBean> managedBeans = managedBeanBnd.getManagedBeans();
        Assert.assertEquals(1, managedBeans.size());
        ManagedBean managedBean = managedBeans.get(0);
        Assert.assertEquals("com.ibm.ManagedBean", managedBean.getClazz());
        return managedBean;
    }

    /**
     * @param xmlSnippet
     * @return
     * @throws Exception
     */
    private Interceptor getInterceptor(String xmlSnippet) throws Exception {
        String xmlString = managedBeanBndXML +
                           xmlSnippet +
                           "</managed-bean-bnd>";

        ManagedBeanBnd managedBeanBnd = getManagedBeanBnd(xmlString);
        List<Interceptor> interceptors = managedBeanBnd.getInterceptors();
        Assert.assertEquals(1, interceptors.size());
        Interceptor interceptor = interceptors.get(0);
        Assert.assertEquals("suite.r70.base.injection.mix.ejbint.XMLInjectionInterceptor3", interceptor.getClassName());
        return interceptor;
    }

    /**
     * Validate that the given {@link RefBindingsGroup} does not contain any refs
     * 
     * @param refBindingsGroup
     */
    private void validateEmptyRefBindings(RefBindingsGroup refBindingsGroup) {
        Assert.assertNotNull(refBindingsGroup.getEJBRefs());
        Assert.assertEquals("EJB refs should be an empty list.", 0, refBindingsGroup.getEJBRefs().size());
        Assert.assertNotNull(refBindingsGroup.getMessageDestinationRefs());
        Assert.assertEquals("Message destination refs should be an empty list.", 0, refBindingsGroup.getMessageDestinationRefs().size());
        Assert.assertNotNull(refBindingsGroup.getResourceEnvRefs());
        Assert.assertEquals("Resource env refs should be an empty list.", 0, refBindingsGroup.getResourceEnvRefs().size());
        Assert.assertNotNull(refBindingsGroup.getResourceRefs());
        Assert.assertEquals("Resource refs should be an empty list.", 0, refBindingsGroup.getResourceRefs().size());
    }

    /**
     * Validate that the given {@link RefBindingsGroup} does contains
     * the expected refs
     * 
     * @param refBindingsGroup
     */
    private void validateRefBindings(RefBindingsGroup refBindingsGroup) {
        Assert.assertNotNull(refBindingsGroup.getEJBRefs());
        Assert.assertEquals(2, refBindingsGroup.getEJBRefs().size());
        Assert.assertEquals("AnnotationInjectionInterceptor/ejbLocalRef", refBindingsGroup.getEJBRefs().get(0).getName());
        Assert.assertEquals("ejblocal:session/MixedSFInterceptorBean/MixedSFLocal", refBindingsGroup.getEJBRefs().get(0).getBindingName());
        Assert.assertEquals("AnnotationInjectionInterceptor/ejbRemoteRef", refBindingsGroup.getEJBRefs().get(1).getName());
        Assert.assertEquals("session/MixedSFInterceptorBean/MixedSFRemote", refBindingsGroup.getEJBRefs().get(1).getBindingName());

        Assert.assertNotNull(refBindingsGroup.getMessageDestinationRefs());
        Assert.assertEquals(1, refBindingsGroup.getMessageDestinationRefs().size());
        Assert.assertEquals("AnnotationInjectionInterceptor/jms/RequestQueue", refBindingsGroup.getMessageDestinationRefs().get(0).getName());
        Assert.assertEquals("Jetstream/jms/RequestQueue", refBindingsGroup.getMessageDestinationRefs().get(0).getBindingName());

        Assert.assertNotNull(refBindingsGroup.getResourceEnvRefs());
        Assert.assertEquals(1, refBindingsGroup.getResourceEnvRefs().size());
        Assert.assertEquals("AnnotationInjectionInterceptor/jms/ResponseQueue", refBindingsGroup.getResourceEnvRefs().get(0).getName());
        Assert.assertEquals("Jetstream/jms/ResponseQueue", refBindingsGroup.getResourceEnvRefs().get(0).getBindingName());

        Assert.assertNotNull(refBindingsGroup.getResourceRefs());
        Assert.assertEquals(1, refBindingsGroup.getResourceRefs().size());
        Assert.assertEquals("AnnotationInjectionInterceptor/jms/WSTestQCF", refBindingsGroup.getResourceRefs().get(0).getName());
        Assert.assertEquals("Jetstream/jms/WSTestQCF", refBindingsGroup.getResourceRefs().get(0).getBindingName());
    }
}