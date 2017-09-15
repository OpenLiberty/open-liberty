/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbbnd.MessageDriven;

public class MessageDrivenTest extends EJBJarBndTestBase {

    String messageDrivenXML1 = "<message-driven name=\"MessageDrivenBean1\"> \n"
                               + "        <listener-port name=\"listenerPortName1\"/> \n "
                               + "</message-driven> \n";

    @Test
    public void testMessageDrivenEmptyLists() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML1 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertNotNull(mdb.getEJBRefs());
        Assert.assertEquals(0, mdb.getEJBRefs().size());
        Assert.assertNotNull(mdb.getMessageDestinationRefs());
        Assert.assertEquals(0, mdb.getMessageDestinationRefs().size());
        Assert.assertNotNull(mdb.getResourceEnvRefs());
        Assert.assertEquals(0, mdb.getResourceEnvRefs().size());
        Assert.assertNotNull(mdb.getResourceRefs());
        Assert.assertEquals(0, mdb.getResourceRefs().size());
    }

    @Test
    public void testMessageDrivenAttributeName() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML1 +
                        "</ejb-jar-bnd>";
        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean1", mdb.getName());
    }

    String messageDrivenXML2 = "<message-driven name=\"MessageDrivenBean2\"> \n"
                               + "        <listener-port name=\"listenerPortName2\"/> \n "
                               + "</message-driven> \n";

    @Test
    public void testMessageDrivenElementListenerPort() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML2 +
                        "</ejb-jar-bnd>";
        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertNotNull(mdb.getListenerPort());
        Assert.assertEquals("listenerPortName2", mdb.getListenerPort().getName());
    }

    String messageDrivenXML3 = "<message-driven name=\"MessageDrivenBean3\"> \n"
                               + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName3\" destination-binding-name=\"DestinationBindingName3\" activation-spec-auth-alias=\"ActivationSpecAuthAlias3\"/> \n"
                               + "</message-driven> \n";

    @Test
    public void testMessageDrivenElementJCAAdapter() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML3 +
                        "</ejb-jar-bnd>";
        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertNotNull(mdb.getJCAAdapter());
        Assert.assertEquals("ActivationSpecAuthAlias3", mdb.getJCAAdapter().getActivationSpecAuthAlias());
        Assert.assertEquals("ActivationSpecBindingName3", mdb.getJCAAdapter().getActivationSpecBindingName());
        Assert.assertEquals("DestinationBindingName3", mdb.getJCAAdapter().getDestinationBindingName());
    }

    String messageDrivenXML4 = "<message-driven name=\"MessageDrivenBean4\"> \n"
                               + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName4\"/> \n"
                               + "<ejb-ref name=\"ejbRefName4\" binding-name=\"ejbRefBindingName4\"/> \n"
                               + "</message-driven> \n";

    @Test
    public void testMessageDrivenElementEjbRef() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML4 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean4", mdb.getName());
        Assert.assertNotNull(mdb.getEJBRefs());
        Assert.assertEquals(1, mdb.getEJBRefs().size());
        Assert.assertEquals("ejbRefName4", mdb.getEJBRefs().get(0).getName());
        Assert.assertEquals("ejbRefBindingName4", mdb.getEJBRefs().get(0).getBindingName());
    }

    String messageDrivenXML5 = "<message-driven name=\"MessageDrivenBean5\">  \n"
                               + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName5\"/> \n"
                               + "<resource-ref name=\"resourceRefName5\" binding-name=\"resourceRefBindingName5\"/>  \n"
                               + "</message-driven>  \n";

    @Test
    public void testMessageDrivenElementResourceRef() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML5 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean5", mdb.getName());
        Assert.assertNotNull(mdb.getResourceRefs());
        Assert.assertEquals(1, mdb.getResourceRefs().size());
        Assert.assertEquals("resourceRefName5", mdb.getResourceRefs().get(0).getName());
        Assert.assertEquals("resourceRefBindingName5", mdb.getResourceRefs().get(0).getBindingName());
    }

    String messageDrivenXML6 = "<message-driven name=\"MessageDrivenBean6\"> \n"
                               + "<listener-port name=\"lpName\"/> \n "
                               + "<resource-ref name=\"ResourceRef6\" binding-name=\"ResourceRefBindingName6\"> "
                               + "<authentication-alias name=\"AuthAlias6\" />"
                               + "<custom-login-configuration name=\"customLoginConfiguration6\"> "
                               + "<property name=\"propname\" value=\"propvalue\"/> \n"
                               + "</custom-login-configuration> \n"
                               + "<default-auth userid=\"testuser\" password=\"testpw\"/> \n "
                               + "</resource-ref>"
                               + "</message-driven> \n";

    @Test
    public void testMessageDrivenElementResourceRefOptionalElements() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML6 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean6", mdb.getName());
        Assert.assertNotNull(mdb.getResourceRefs());
        Assert.assertEquals("ResourceRef6", mdb.getResourceRefs().get(0).getName());
        Assert.assertEquals("ResourceRefBindingName6", mdb.getResourceRefs().get(0).getBindingName());
        Assert.assertEquals("AuthAlias6", mdb.getResourceRefs().get(0).getAuthenticationAlias().getName());
        Assert.assertEquals("customLoginConfiguration6", mdb.getResourceRefs().get(0).getCustomLoginConfiguration().getName());
        Assert.assertEquals(1, mdb.getResourceRefs().get(0).getCustomLoginConfiguration().getProperties().size());
        Assert.assertEquals("propname", mdb.getResourceRefs().get(0).getCustomLoginConfiguration().getProperties().get(0).getName());
        Assert.assertEquals("propvalue", mdb.getResourceRefs().get(0).getCustomLoginConfiguration().getProperties().get(0).getValue());
        Assert.assertEquals("testuser", mdb.getResourceRefs().get(0).getDefaultAuthUserid());
        Assert.assertEquals("testpw", mdb.getResourceRefs().get(0).getDefaultAuthPassword());
    }

    /*
     * String messageDrivenXML7 = "<message-driven name=\"MessageDrivenBean7\"> \n"
     * + "  <listener-port name=\"lpName\"/> \n "
     * + "  <resource-ref name=\"ResourceRef7a\" binding-name=\"ResourceRefBindingName7a\"/>  \n"
     * + "  <resource-ref name=\"ResourceRef7b\" binding-name=\"ResourceRefBindingName7b\"/>  \n"
     * + "</message-driven>  \n";
     */

    @Test
    public void testMessageDrivenElementResourceRefMultiple() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML7 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean7", mdb.getName());
        Assert.assertNotNull(mdb.getResourceRefs());
        Assert.assertEquals("Resource refs should have 2 entries.", 2, mdb.getResourceRefs().size());
        Assert.assertEquals("ResourceRef7a", mdb.getResourceRefs().get(0).getName());
        Assert.assertEquals("ResourceRefBindingName7a", mdb.getResourceRefs().get(0).getBindingName());
        Assert.assertEquals("ResourceRef7b", mdb.getResourceRefs().get(1).getName());
        Assert.assertEquals("ResourceRefBindingName7b", mdb.getResourceRefs().get(1).getBindingName());

    }

    String messageDrivenXML8 = "<message-driven name=\"MessageDrivenBean8\">  \n"
                               + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName8\"/> \n"
                               + "<resource-env-ref name=\"ResourceEnvRefName8\" binding-name=\"ResourceEnvRefBindingName8\"/>  \n"
                               + "</message-driven>  \n";

    @Test
    public void testMessageDrivenElementResourceEnvRef() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML8 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean8", mdb.getName());
        Assert.assertNotNull(mdb.getResourceEnvRefs());
        Assert.assertEquals("Resource env ref list should have 1 element.", 1, mdb.getResourceEnvRefs().size());
        Assert.assertEquals("ResourceEnvRefName8", mdb.getResourceEnvRefs().get(0).getName());
        Assert.assertEquals("ResourceEnvRefBindingName8", mdb.getResourceEnvRefs().get(0).getBindingName());
    }

    /*
     * String messageDrivenXML9 = "<message-driven name=\"MessageDrivenBean9\">  \n"
     * + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName9\"/> \n"
     * + "<resource-env-ref name=\"ResourceEnvRefName9a\" binding-name=\"ResourceEnvRefBindingName9a\"/>  \n"
     * + "<resource-env-ref name=\"ResourceEnvRefName9b\" binding-name=\"ResourceEnvRefBindingName9b\"/>  \n"
     * + "</message-driven>  \n";
     */

    @Test
    public void testMessageDrivenElementResourceEnvRefMultiple() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML9 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean9", mdb.getName());
        Assert.assertNotNull(mdb.getResourceEnvRefs());
        Assert.assertEquals("Resource env ref list should have 2 entries.", 2, mdb.getResourceEnvRefs().size());
        Assert.assertEquals("ResourceEnvRefName9a", mdb.getResourceEnvRefs().get(0).getName());
        Assert.assertEquals("ResourceEnvRefBindingName9a", mdb.getResourceEnvRefs().get(0).getBindingName());
        Assert.assertEquals("ResourceEnvRefName9b", mdb.getResourceEnvRefs().get(1).getName());
        Assert.assertEquals("ResourceEnvRefBindingName9b", mdb.getResourceEnvRefs().get(1).getBindingName());
    }

    String messageDrivenXML10 = "<message-driven name=\"MessageDrivenBean10\">  \n"
                                + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName10\"/> \n"
                                + " <message-destination-ref name=\"MessageDestinationRefName10\" binding-name=\"MessageDestinationRefBindingName10\" />   \n"
                                + "</message-driven>  \n";

    @Test
    public void testMessageDrivenElementMessageDestinationRef() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML10 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven mdb = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean10", mdb.getName());
        Assert.assertNotNull(mdb.getMessageDestinationRefs());
        Assert.assertEquals(1, mdb.getMessageDestinationRefs().size());
        Assert.assertEquals("MessageDestinationRefName10", mdb.getMessageDestinationRefs().get(0).getName());
        Assert.assertEquals("MessageDestinationRefBindingName10", mdb.getMessageDestinationRefs().get(0).getBindingName());
    }

    @Test
    public void testMessageDrivenMultiple() throws Exception {
        String mdbXML = EJBJarBndTestBase.ejbJarBnd11() +
                        messageDrivenXML1 +
                        messageDrivenXML2 +
                        messageDrivenXML3 +
                        messageDrivenXML4 +
                        messageDrivenXML5 +
                        messageDrivenXML6 +
                        messageDrivenXML7 +
                        messageDrivenXML8 +
                        messageDrivenXML9 +
                        messageDrivenXML10 +
                        "</ejb-jar-bnd>";

        EJBJarBnd ejbJarBnd = getEJBJarBnd(mdbXML);
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(10, mdBeans.size());
    }
}
