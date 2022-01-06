/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestination;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;


@RunWith(Parameterized.class)
public class EJBJarBndTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }

    public EJBJarBndTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    //
    
    @Test
    public void testGetVersionID() throws Exception {
        Assert.assertEquals(
                "XMI",
                parseEJBJarBndXMI(ejbJarBndXMI(), getEJBJar21()).getVersion());

        Assert.assertEquals(
                "1.0",
                parseEJBJarBndXML(ejbJarBnd10()).getVersion());
        
        Assert.assertEquals(
                "1.1",
                parseEJBJarBndXML(ejbJarBnd11()).getVersion());
        
        Assert.assertEquals(
                "1.2",
                parseEJBJarBndXML(ejbJarBnd12()).getVersion());

    }

    @Test
    public void testEnterpriseBeans() throws Exception {
        Assert.assertNotNull("Enterprise bean list should not be null.",
                parseEJBJarBndXML(ejbJarBnd10()).getEnterpriseBeans());
        Assert.assertEquals("Enterprise bean list should not be empty.", 1,
                parseEJBJarBndXML(ejbJarBnd10()).getEnterpriseBeans().size());
    }

    @Test
    public void testEmptyMessageDestinations() throws Exception {
        Assert.assertNotNull("MessageDestinations list should not be null.",
                parseEJBJarBndXML(ejbJarBnd10()).getMessageDestinations());
        Assert.assertEquals("MessageDestinations list should be empty.",
                0,
                parseEJBJarBndXML(ejbJarBnd10()).getMessageDestinations().size());
    }

    @Test
    public void testEmptyInterceptorsList() throws Exception {
        Assert.assertNotNull("Interceptor list should not be null.",
                parseEJBJarBndXML(ejbJarBnd10()).getInterceptors());
        Assert.assertEquals("Interceptor list should be empty.",
                0,
                parseEJBJarBndXML(ejbJarBnd10()).getInterceptors().size());
    }

    @Test
    public void testEmptyXMI() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXMI( ejbJarBndXMI("", ""), getEJBJar21() );

        Assert.assertNotNull("Enterprise bean list should not be null.", ejbJarBnd.getEnterpriseBeans());
        Assert.assertEquals("Enterprise bean list should be empty.", 0, ejbJarBnd.getEnterpriseBeans().size());
        Assert.assertNotNull("MessageDestinations list should not be null.", ejbJarBnd.getMessageDestinations());
        Assert.assertEquals("MessageDestinations list should be empty.", 0, ejbJarBnd.getMessageDestinations().size());
        Assert.assertNotNull("Interceptor list should not be null.", ejbJarBnd.getInterceptors());
        Assert.assertEquals("Interceptor list should be empty.", 0, ejbJarBnd.getInterceptors().size());
    }

    // Interceptor and message destination tests are included here since the InterceptorType is from commonbnd. 
    // The other EJB type tests are in their specific type test file in this package.
    public static final String interceptorXML2 =
            "<interceptor class=\"com.ibm.test.InterceptorClass\">\n" +
                    "<ejb-ref name=\"ejbRef2a\" binding-name=\"ejbRefBindingName2a\"/>\n" +
                    "<ejb-ref name=\"ejbRef2b\" binding-name=\"ejbRefBindingName2b\"/>\n" +
                    "<resource-ref name=\"resourceRefName\" binding-name=\"resourceRefBindingName\"/>\n" +
                    "<message-destination-ref name=\"messageDestName\" binding-name=\"messageDestBindingName\"/>\n" +
                    "<resource-env-ref name=\"resourceEnvRefName\" binding-name=\"resourceEnvRefBindingName\"/>\n" +
            "</interceptor>\n";

    @Test
    public void testInterceptorEmptyLists() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(interceptorXML1));
        
        List<Interceptor> interceptors = ejbJarBnd.getInterceptors();
        Assert.assertEquals(1, interceptors.size());
        Interceptor interceptor = interceptors.get(0);
        Assert.assertEquals("com.ibm.test.Interceptor1", interceptor.getClassName());
        Assert.assertNotNull(interceptor.getEJBRefs());
        Assert.assertEquals("EJB refs should be an empty list.", 0, interceptor.getEJBRefs().size());
        Assert.assertNotNull(interceptor.getMessageDestinationRefs());
        Assert.assertEquals("Message destination refs should be an empty list.", 0, interceptor.getMessageDestinationRefs().size());
        Assert.assertNotNull(interceptor.getResourceEnvRefs());
        Assert.assertEquals("Resource env refs should be an empty list.", 0, interceptor.getResourceEnvRefs().size());
        Assert.assertNotNull(interceptor.getResourceRefs());
        Assert.assertEquals("Resource refs should be an empty list.", 0, interceptor.getResourceRefs().size());
    }

    @Test
    public void testInterceptorLists() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(interceptorXML2));
        
        List<Interceptor> interceptors = ejbJarBnd.getInterceptors();
        Assert.assertEquals(1, interceptors.size());
        Interceptor interceptor = interceptors.get(0);
        Assert.assertEquals("com.ibm.test.InterceptorClass", interceptor.getClassName());

        Assert.assertNotNull(interceptor.getEJBRefs());
        Assert.assertEquals(2, interceptor.getEJBRefs().size());
        Assert.assertEquals("ejbRef2a", interceptor.getEJBRefs().get(0).getName());
        Assert.assertEquals("ejbRefBindingName2a", interceptor.getEJBRefs().get(0).getBindingName());
        Assert.assertEquals("ejbRef2b", interceptor.getEJBRefs().get(1).getName());
        Assert.assertEquals("ejbRefBindingName2b", interceptor.getEJBRefs().get(1).getBindingName());

        Assert.assertNotNull(interceptor.getMessageDestinationRefs());
        Assert.assertEquals(1, interceptor.getMessageDestinationRefs().size());
        Assert.assertEquals("messageDestName", interceptor.getMessageDestinationRefs().get(0).getName());
        Assert.assertEquals("messageDestBindingName", interceptor.getMessageDestinationRefs().get(0).getBindingName());

        Assert.assertNotNull(interceptor.getResourceEnvRefs());
        Assert.assertEquals(1, interceptor.getResourceEnvRefs().size());
        Assert.assertEquals("resourceEnvRefName", interceptor.getResourceEnvRefs().get(0).getName());
        Assert.assertEquals("resourceEnvRefBindingName", interceptor.getResourceEnvRefs().get(0).getBindingName());

        Assert.assertNotNull(interceptor.getResourceRefs());
        Assert.assertEquals(1, interceptor.getResourceRefs().size());
        Assert.assertEquals("resourceRefName", interceptor.getResourceRefs().get(0).getName());
        Assert.assertEquals("resourceRefBindingName", interceptor.getResourceRefs().get(0).getBindingName());
    }

    public static final String messageDetinationXML2 =
            "<message-destination name=\"messageDestName2\" binding-name=\"messageDestBinding2\">\n" +
            "</message-destination>\n";

    @Test
    public void testMessageDestinations() throws Exception {
        String messageDestXML = ejbJarBnd11(
                messageDetinationXML1 +
                messageDetinationXML2);
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(messageDestXML);

        List<MessageDestination> messageDests = ejbJarBnd.getMessageDestinations();
        Assert.assertEquals(2, messageDests.size());
        Assert.assertEquals("messageDestName1", messageDests.get(0).getName());
        Assert.assertEquals("messageDestBinding1", messageDests.get(0).getBindingName());
        Assert.assertEquals("messageDestName2", messageDests.get(1).getName());
        Assert.assertEquals("messageDestBinding2", messageDests.get(1).getBindingName());
    }

    @Test
    public void testEjbJarBndMultiple() throws Exception {
        String mdbXML = ejbJarBnd11(
                sessionXML8 +
                sessionXML11 +
                messageDrivenXML7 +
                messageDrivenXML9 +
                interceptorXML1 +
                interceptorXML2 +
                messageDetinationXML1 +
                messageDetinationXML2);
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(mdbXML);

        Assert.assertEquals(4, ejbJarBnd.getEnterpriseBeans().size());
        Assert.assertEquals(2, ejbJarBnd.getInterceptors().size());
        Assert.assertEquals(2, ejbJarBnd.getMessageDestinations().size());

        // make sure objects do not get doubled when called twice
        Assert.assertEquals(4, ejbJarBnd.getEnterpriseBeans().size());
        Assert.assertEquals(2, ejbJarBnd.getInterceptors().size());
        Assert.assertEquals(2, ejbJarBnd.getMessageDestinations().size());
    }

    @Test
    public void testCurrentBackendID() throws Exception {
        parseEJBJarBndXMI(ejbBndXMLCurrentBackendID, getEJBJar21());
    }

    @Test
    public void testDefaultCMPConnectionFactory() throws Exception {
        parseEJBJarBndXMI( ejbJarBndXMI("", defaultCMPConnectionFactoryXMI1),
                           getEJBJar21() );
    }

    @Test
    public void testCMPConnectionFactory() throws Exception {
        parseEJBJarBndXMI( ejbJarBndXMI("", testCMPConnectionFactoryXMI1()),
                           getEJBJar21() );
    }

    @Test
    public void testDefaultDataSource() throws Exception {
        parseEJBJarBndXMI( ejbJarBndXMI("", defaultDataSourceXMI1),
                           getEJBJar21() );
    }

    @Test
    public void testDefaultDataSource2() throws Exception {
        parseEJBJarBndXMI( ejbJarBndXMI("", defaultDataSourceXMI2),
                           getEJBJar21() );
    }
}
