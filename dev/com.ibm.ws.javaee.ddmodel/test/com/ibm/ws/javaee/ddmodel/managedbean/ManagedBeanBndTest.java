/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.managedbean;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup;
import com.ibm.ws.javaee.dd.managedbean.ManagedBean;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

@RunWith(Parameterized.class)
public class ManagedBeanBndTest extends DDTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
        
    public ManagedBeanBndTest(boolean isWar) {
        this.isWar = isWar;
    }

    private final boolean isWar;

    public boolean getIsWar() {
        return isWar;
    }   

    public static String getMBeanBndPath(boolean isWar) {
        return ( isWar ? ManagedBeanBndAdapter.XML_BND_IN_WEB_MOD_NAME
                       : ManagedBeanBndAdapter.XML_BND_IN_EJB_MOD_NAME );    
    }
    
    public String getMBeanBndPath() {
        return getMBeanBndPath( getIsWar() );
    }


    // XML fragments ...

    private static final String refsXML =
        "<ejb-ref name=\"AnnotationInjectionInterceptor/ejbLocalRef\"" +
            " binding-name=\"ejblocal:session/MixedSFInterceptorBean/MixedSFLocal\"/>\n" +
        "<ejb-ref name=\"AnnotationInjectionInterceptor/ejbRemoteRef\"" +
            " binding-name=\"session/MixedSFInterceptorBean/MixedSFRemote\"/>\n" +
        "<resource-ref name=\"AnnotationInjectionInterceptor/jms/WSTestQCF\"" +
            " binding-name=\"Jetstream/jms/WSTestQCF\"/>\n" +
        "<message-destination-ref name=\"AnnotationInjectionInterceptor/jms/RequestQueue\"" +
            " binding-name=\"Jetstream/jms/RequestQueue\"/>\n" +
        "<resource-env-ref name=\"AnnotationInjectionInterceptor/jms/ResponseQueue\"" +
            " binding-name=\"Jetstream/jms/ResponseQueue\"/>";

    private final static String interceptorXML_Start =
        "<interceptor class=\"suite.r70.base.injection.mix.ejbint.XMLInjectionInterceptor3\">";
    private final static String interceptorXML_End =
        "</interceptor>";

    private static String interceptorXML(String nestedXMLText) {
        return interceptorXML_Start +
               nestedXMLText +
               interceptorXML_End;
    }
    
    private final static String mBeanXML_Start =
        "<managed-bean id=\"managedBeanID\" class=\"com.ibm.ManagedBean\">\n";
    private final static String mBeanXML_End =            
        "</managed-bean>\n";

    private static String mBeanXML(String nestedXMLText) {
        return mBeanXML_Start +
               nestedXMLText +
               mBeanXML_End;
    }
    
    private final static String noClassMBeanXML =
        "<managed-bean id=\"managedBeanID\">\n" +
        "</managed-bean>\n";

    //
    
    private final static String mBeanBndXML_Start =
        "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\"" +
            " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd\">";

    private static final String mBeanBndXML_End =
        "</managed-bean-bnd>";
    
    private static String mBeanBndXML(String nestedXMLText) {
        return mBeanBndXML_Start + "\n" +
                   nestedXMLText +
               mBeanBndXML_End + "\n";
    }
    
    private final static String version11MBeanBndXML =
            "<managed-bean-bnd id=\"idvalue0\" version=\"1.1\"" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_1.xsd\">" +
            "</managed-bean-bnd>";

    private final static String noVersionMBeanBndXML =
            "<managed-bean-bnd id=\"idvalue0\"" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd\">" +
            "</managed-bean-bnd>";

    private final static String noNamespaceMBeanBndXML =
        "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_1.xsd\">" +
        "</managed-bean-bnd>";    
    
    private final static String noSchemaInstanceMBeanBndXML =
        "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\"" +
            " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_1.xsd\">" +
        "</managed-bean-bnd>";    
    
    private final static String noSchemaLocationMBeanBndXML =
        "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\"" +
            " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
        "</managed-bean-bnd>";
    
    private final static String badRootMBeanXML =
        "<managed-bean-bnd-wrong id=\"idvalue0\"" +
            "xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd\">" +
        "</managed-bean-bnd-wrong>";

    private final static String versionOnlyMBeanBndXML =
            "<managed-bean-bnd id=\"idvalue0\" version=\"1.0\"" +
                // " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee ibm-managed-bean-bnd_1_0.xsd\"" +
                ">" +
            "</managed-bean-bnd>";    
    //
    
    private ManagedBeanBnd parseBnd(String ddText) throws Exception {
        return parseBnd(ddText, null);
    }

    private ManagedBeanBnd parseBnd(String ddText, String altMessage, String ... messages) throws Exception {
        String appPath = null;
        
        String modulePath;
        WebModuleInfo webInfo;
        if ( getIsWar() ) {
            modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
            webInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);
        } else {
            modulePath = "/root/wlp/usr/servers/server1/apps/MyEjb.jar";
            webInfo = null;
        }

        String fragmentPath = null;

        String ddPath = getMBeanBndPath();

        return parse( appPath, modulePath, fragmentPath,
                ddText, new ManagedBeanBndAdapter(), ddPath,
                null, null,
                WebModuleInfo.class, webInfo,
                altMessage, messages );
    }

    // Parse helpers ...

    private ManagedBean parseMBean(String nestedDDText) throws Exception {
        String ddText = mBeanBndXML(nestedDDText);
        ManagedBeanBnd managedBeanBnd = parseBnd(ddText);

        List<ManagedBean> managedBeans = managedBeanBnd.getManagedBeans();
        Assert.assertEquals(1, managedBeans.size());
        ManagedBean managedBean = managedBeans.get(0);
        Assert.assertEquals("com.ibm.ManagedBean", managedBean.getClazz());

        return managedBean;
    }

    private Interceptor parseInterceptor(String nestedDDText) throws Exception {
        String ddText = mBeanBndXML(nestedDDText);
        ManagedBeanBnd managedBeanBnd = parseBnd(ddText);

        List<Interceptor> interceptors = managedBeanBnd.getInterceptors();
        Assert.assertEquals(1, interceptors.size());
        Interceptor interceptor = interceptors.get(0);
        Assert.assertEquals("suite.r70.base.injection.mix.ejbint.XMLInjectionInterceptor3",
                            interceptor.getClassName());

        return interceptor;
    }
        
    // Tests on bad data ...
    
    @Test
    public void testWrongRootElement() throws Exception {
        parseBnd(badRootMBeanXML, "xml.error", "CWWKC2272E");
    }

    @Test
    public void testNamespaceMissing() throws Exception {
        parseBnd(noNamespaceMBeanBndXML);
    }
    
    @Test
    public void testSchemaInstanceMissing() throws Exception {
        parseBnd(noSchemaInstanceMBeanBndXML, "xml.error", "CWWKC2272E"); 
    }

    // Current parsing does not require a schema location.
    @Test
    public void testSchemaLocationMissing() throws Exception {
        parseBnd(noSchemaLocationMBeanBndXML);
    }

    @Test
    public void testVersionMissing() throws Exception {
        parseBnd(noVersionMBeanBndXML);
    }

    @Test
    public void testVersionOnly() throws Exception {
        parseBnd(versionOnlyMBeanBndXML);
    }
    
    //

    @Test
    public void testRequiredAttributeClassMissing() throws Exception {
        parseBnd( mBeanBndXML(noClassMBeanXML), "required.attribute.missing", "CWWKC2251E" );
    }

    // Tests on valid data ...

    @Test
    public void testEmptyBeansList() throws Exception {
        ManagedBeanBnd managedBeanBnd = parseBnd( mBeanBndXML("") );
        Assert.assertNotNull("managed bean list should not be null.",
            managedBeanBnd.getManagedBeans());
        Assert.assertEquals("managed bean list should be empty.",
            0, managedBeanBnd.getManagedBeans().size());
    }

    @Test
    public void testManagedBeanWithNoData() throws Exception {
        ManagedBean managedBean = parseMBean( mBeanXML("") );
        validateEmptyRefBindings(managedBean);
    }

    @Test
    public void testManagedBeanWithData() throws Exception {
        ManagedBean managedBean = parseMBean( mBeanXML(refsXML) );
        validateRefBindings(managedBean);
    }

    @Test
    public void testManagedBeanVersion11() throws Exception {
        ManagedBeanBnd managedBeanBnd = parseBnd(version11MBeanBndXML);
        Assert.assertEquals("Managed beans binding should be of version 1.1.",
            "1.1", managedBeanBnd.getVersion());
    }

    @Test
    public void testEmptyInterceptorsList() throws Exception {
        ManagedBeanBnd managedBeanBnd = parseBnd( mBeanBndXML("") );
        Assert.assertNotNull("Interceptor list should not be null.",
            managedBeanBnd.getInterceptors());
        Assert.assertEquals("Interceptor list should be empty.",
            0, managedBeanBnd.getInterceptors().size());
    }

    @Test
    public void testInterceptorWithNoData() throws Exception {
        Interceptor interceptor = parseInterceptor( interceptorXML("") );
        validateEmptyRefBindings(interceptor);
    }

    @Test
    public void testInterceptorWithData() throws Exception {
        Interceptor interceptor = parseInterceptor( interceptorXML(refsXML) );
        validateRefBindings(interceptor);
    }

    /**
     * Verify that the given {@link RefBindingsGroup} does not contain any references.
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
     * Verify that the given {@link RefBindingsGroup} contains expected references.
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
