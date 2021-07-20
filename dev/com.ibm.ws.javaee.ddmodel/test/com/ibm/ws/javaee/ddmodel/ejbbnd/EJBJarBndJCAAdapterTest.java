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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbbnd.MessageDriven;

@RunWith(Parameterized.class)
public class EJBJarBndJCAAdapterTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarBndJCAAdapterTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    protected static final String jcaAdapterXML1 =
            "<message-driven name=\"MessageDrivenBean1\">\n" +
                "<jca-adapter activation-spec-binding-name=\"activationSpecBindingName1\"/>\n" +
            "</message-driven>";

    @Test
    public void testJCAAdapterAttributeActivationSpecBindingName() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(EJBJarBndTestBase.ejbJarBnd11(jcaAdapterXML1));

        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals("Only expected 1 message driven bean", 1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean1", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertNotNull(bean0.getJCAAdapter().getActivationSpecBindingName());
        Assert.assertEquals("activationSpecBindingName1", bean0.getJCAAdapter().getActivationSpecBindingName());
    }

    @Test
    public void testJCAAdapterAttributeActivationSpecBindingNameXMI() throws Exception {
        EJBJar ejbJar = parseEJBJar(
                ejbJar21(
                    "<enterprise-beans>" +
                        "<message-driven id=\"md0\">" +
                            "<ejb-name>MessageDrivenBean1</ejb-name>" +
                        "</message-driven>" +
                    "</enterprise-beans>"));

        EJBJarBnd ejbJarBnd = parseEJBJarBndXMI(
                ejbJarBndXMI("",
                    "<ejbBindings xmi:type=\"ejbbnd:MessageDrivenBeanBinding\" activationSpecJndiName=\"activationSpecBindingName1\">" +
                        "<enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"" + getEJBJarPath() + "#md0\"/>" +
                    "</ejbBindings>"),
                ejbJar);

        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals("Only expected 1 message driven bean", 1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean1", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertNotNull(bean0.getJCAAdapter().getActivationSpecBindingName());
        Assert.assertEquals("activationSpecBindingName1", bean0.getJCAAdapter().getActivationSpecBindingName());
    }

    protected static final String jcaAdapterXML2 =
            "<message-driven name=\"MessageDrivenBean2\">\n" +
                "<jca-adapter" +
                    " activation-spec-binding-name=\"activationSpecBindingName1\"\n" +
                    " activation-spec-auth-alias=\"authAlias2\"/>\n" +
            "</message-driven>\n";

    @Test
    public void testJCAAdapterAttributeActivationSpecAuthAlias() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(
                EJBJarBndTestBase.ejbJarBnd10(jcaAdapterXML2));
        
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean2", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertEquals("authAlias2", bean0.getJCAAdapter().getActivationSpecAuthAlias());
    }

    @Test
    public void testJCAAdapterAttributeActivationSpecAuthAliasXMI() throws Exception {
        EJBJar ejbJar = parseEJBJar(ejbJar21(
                        "<enterprise-beans>" +
                            "<message-driven id=\"md0\">" +
                                "<ejb-name>MessageDrivenBean2</ejb-name>" +
                            "</message-driven>" +
                        "</enterprise-beans>"));
        
        EJBJarBnd ejbJarBnd = parseEJBJarBndXMI(
                ejbJarBndXMI("",
                    "<ejbBindings xmi:type=\"ejbbnd:MessageDrivenBeanBinding\" activationSpecAuthAlias=\"authAlias2\">" +
                        "<enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"" + getEJBJarPath() + "#md0\"/>" +
                    "</ejbBindings>"),
                ejbJar);

        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals(1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean2", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertEquals("authAlias2", bean0.getJCAAdapter().getActivationSpecAuthAlias());
    }

    protected static final String jcaAdapterXML3 =
            "<message-driven name=\"MessageDrivenBean3\">\n" +
                "<jca-adapter activation-spec-binding-name=\"activationSpecBindingName3\" destination-binding-name=\"destinationBindingName\"/>\n" +
            "</message-driven>";

    @Test
    public void testJCAAdapterAttributeDestinationBindingName() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd10(jcaAdapterXML3));

        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals("Only expected 1 message driven bean", 1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean3", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertNotNull(bean0.getJCAAdapter().getActivationSpecBindingName());
        Assert.assertEquals("destinationBindingName", bean0.getJCAAdapter().getDestinationBindingName());
    }

    @Test
    public void testJCAAdapterAttributeDestinationBindingNameXMI() throws Exception {
        EJBJar ejbJar = parseEJBJar(
                ejbJar21(
                    "<enterprise-beans>" +
                        "<message-driven id=\"md0\">" +
                            "<ejb-name>MessageDrivenBean3</ejb-name>" +
                        "</message-driven>" +
                    "</enterprise-beans>"));


        EJBJarBnd ejbJarBnd = parseEJBJarBndXMI(
                ejbJarBndXMI("",
                    "<ejbBindings xmi:type=\"ejbbnd:MessageDrivenBeanBinding\" " +
                        "activationSpecJndiName=\"activationSpecBindingName3\" " +
                        "destinationJndiName=\"destinationBindingName\">" +
                        "<enterpriseBean xmi:type=\"ejb:MessageDriven\" href=\"" + getEJBJarPath() + "#md0\"/>" +
                    "</ejbBindings>"),
                ejbJar);
        
        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals("Only expected 1 message driven bean", 1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals("MessageDrivenBean3", bean0.getName());
        Assert.assertNull(bean0.getListenerPort());
        Assert.assertNotNull(bean0.getJCAAdapter());
        Assert.assertNotNull(bean0.getJCAAdapter().getActivationSpecBindingName());
        Assert.assertEquals("destinationBindingName", bean0.getJCAAdapter().getDestinationBindingName());
    }
}
